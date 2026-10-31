"""Run compatibility behavior tests without booting Minecraft or downloading Forge."""
import json
import re
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(os.environ['JAVA_HOME']) / 'bin' if 'JAVA_HOME' in os.environ else Path(shutil.which('javac')).parent
STUBS = {
    'net.minecraft.util.math.BlockPos': '''
        public final int x, y, z;
        public BlockPos(int x, int y, int z) { this.x=x; this.y=y; this.z=z; }
        public BlockPos add(int x, int y, int z) { return new BlockPos(this.x+x,this.y+y,this.z+z); }
        public int getX() { return x; } public int getY() { return y; } public int getZ() { return z; }
        public boolean equals(Object o) { if (!(o instanceof BlockPos)) return false;
            BlockPos p=(BlockPos)o; return x==p.x && y==p.y && z==p.z; }
        public int hashCode() { return x*961+y*31+z; }
    ''',
    'net.minecraft.tileentity.TileEntity': '''
        public net.minecraft.world.World world;
        public net.minecraft.util.math.BlockPos pos;
        public net.minecraft.util.math.BlockPos getPos() { return pos; }
        public net.minecraft.world.World getWorld() { return world; }
    ''',
    'net.minecraft.world.IBlockAccess': '''
        net.minecraft.tileentity.TileEntity getTileEntity(net.minecraft.util.math.BlockPos pos);
    ''',
    'net.minecraft.world.World': '''
        public final java.util.Map<net.minecraft.util.math.BlockPos,net.minecraft.tileentity.TileEntity> tiles=new java.util.HashMap<>();
        public final java.util.List<net.minecraft.tileentity.TileEntity> loadedTileEntityList=new java.util.ArrayList<>();
        public final java.util.Set<net.minecraft.util.math.BlockPos> unloaded=new java.util.HashSet<>();
        public final Provider provider=new Provider();
        public boolean isRemote;
        public net.minecraft.tileentity.TileEntity getTileEntity(net.minecraft.util.math.BlockPos p) { return tiles.get(p); }
        public boolean isBlockLoaded(net.minecraft.util.math.BlockPos p) { return !unloaded.contains(p); }
        public static class Provider { public int dimension; public int getDimension() { return dimension; } }
    ''',
    'net.minecraft.world.WorldServer': '',
    'micdoodle8.mods.galacticraft.api.entity.IDockable': '',
    'micdoodle8.mods.galacticraft.api.tile.IFuelDock': '''
        micdoodle8.mods.galacticraft.api.entity.IDockable getDockedEntity();
    ''',
    'micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket': '''
        public net.minecraft.world.World world;
        public net.minecraft.util.math.BlockPos targetVec;
        public int targetDimension;
        public int updates;
        public micdoodle8.mods.galacticraft.api.tile.IFuelDock updatedPad;
        public void updateControllerSettings(micdoodle8.mods.galacticraft.api.tile.IFuelDock pad) { updates++; updatedPad=pad; }
    ''',
    'micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController': '''
        public int frequency;
        public Object attachedDock;
        public int ticks;
        public String getOwnerName() { return "test"; }
    ''',
    'galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad': '''
        public micdoodle8.mods.galacticraft.api.entity.IDockable rocket;
        public final java.util.Set<Object> connected=new java.util.HashSet<>();
        public micdoodle8.mods.galacticraft.api.entity.IDockable getDockedEntity() { return rocket; }
        public java.util.Set<Object> getConnectedTiles() { return connected; }
    ''',
    'micdoodle8.mods.galacticraft.core.util.GCCoreUtil': '''
        public static net.minecraft.world.WorldServer[] worlds;
        public static net.minecraft.world.WorldServer[] getWorldServerList(net.minecraft.world.World world) { return worlds; }
    ''',
}
STUBS.update({
    'net.minecraft.util.math.ChunkPos': """
        public final int x,z;
        public ChunkPos(BlockPos p) { x=p.x>>4; z=p.z>>4; }
        public boolean equals(Object o) { return o instanceof ChunkPos && ((ChunkPos)o).x==x && ((ChunkPos)o).z==z; }
        public int hashCode() { return x*31+z; }
    """,
    'net.minecraftforge.common.ForgeChunkManager': """
        public static class Ticket { }
        public static void forceChunk(Ticket t, net.minecraft.util.math.ChunkPos p) { }
        public static void unforceChunk(Ticket t, net.minecraft.util.math.ChunkPos p) { }
    """,
    'micdoodle8.mods.galacticraft.core.world.ChunkLoadingCallback': """
        public static void forceChunk(net.minecraftforge.common.ForgeChunkManager.Ticket t,
            net.minecraft.world.World w, int x,int y,int z,String owner) { }
        public static void addToList(net.minecraft.world.World w, int x,int y,int z,String owner) { }
    """,
    'micdoodle8.mods.galacticraft.planets.mars.ConfigManagerMars': 'public static boolean launchControllerChunkLoad=true;',
    'org.spongepowered.asm.mixin.injection.callback.CallbackInfo': 'public void cancel() { }',
    'org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable': 'public T getReturnValue() { return null; } public void setReturnValue(T value) { }',
})
ANNOTATIONS = {
    'org.spongepowered.asm.mixin.Mixin': 'Class<?>[] value(); boolean remap() default true;',
    'org.spongepowered.asm.mixin.Shadow': '',
    'org.spongepowered.asm.mixin.Unique': '',
    'org.spongepowered.asm.mixin.injection.At': 'String value();',
    'org.spongepowered.asm.mixin.injection.Inject': 'String[] method(); At at(); boolean cancellable() default false; int require() default -1;',
}
INTERFACES = {'net.minecraft.world.IBlockAccess', 'micdoodle8.mods.galacticraft.api.entity.IDockable', 'micdoodle8.mods.galacticraft.api.tile.IFuelDock'}
EXTENDS = {
    'net.minecraft.world.World': ' implements IBlockAccess',
    'net.minecraft.world.WorldServer': ' extends World',
    'micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket': ' implements micdoodle8.mods.galacticraft.api.entity.IDockable',
    'micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController': ' extends net.minecraft.tileentity.TileEntity',
    'galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad': ' extends net.minecraft.tileentity.TileEntity implements micdoodle8.mods.galacticraft.api.tile.IFuelDock',
}
with tempfile.TemporaryDirectory(prefix='gs-controller-') as temporary:
    base = Path(temporary)
    sources = []
    for name, body in STUBS.items():
        package, cls = name.rsplit('.', 1)
        source = base / (name.replace('.', '/') + '.java')
        source.parent.mkdir(parents=True, exist_ok=True)
        kind = 'interface' if name in INTERFACES else 'class'
        if cls == 'CallbackInfoReturnable':
            cls += '<T>'
        source.write_text(f'package {package}; public {kind} {cls}{EXTENDS.get(name, "")} {{ {body} }}')
        sources.append(str(source))
    for name, body in ANNOTATIONS.items():
        package, cls = name.rsplit('.', 1)
        source = base / (name.replace('.', '/') + '.java')
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_text(f'package {package}; public @interface {cls} {{ {body} }}')
        sources.append(str(source))
    sources += [str(ROOT / 'src/main/java/galaxyspace/core/mixins'/f'{name}.java')
                for name in ('MixinTileEntityLaunchConroller', 'MixinEntityAutoRocket')]
    sources += [str(ROOT / 'src/main/java/galaxyspace/core/util/LaunchControllerCompatibility.java'),
                str(Path(__file__).with_name('CompatibilityTest.java'))]
    subprocess.run([str(JAVA/'javac'), '-d', str(base), *sources], check=True)
    subprocess.run([str(JAVA/'java'), '-cp', str(base), 'CompatibilityTest'], check=True)

config = json.loads((ROOT/'src/main/resources/mixins.galaxyspace.json').read_text())
for name in ('MixinTileEntityLaunchConroller', 'MixinEntityAutoRocket'):
    assert name in config['mixins'], f'{name} must be registered on both client and server'
print('Changed Java sources compile against API stubs: PASS')
print('Mixin registration: PASS')

# Check selectors against the actual SRG-named GC dependency, not just development names.
for mixin, target in (
    ('MixinTileEntityLaunchConroller', 'micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController'),
    ('MixinEntityAutoRocket', 'micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket'),
):
    methods = subprocess.check_output([str(JAVA/'javap'), '-p', '-classpath',
        str(ROOT/'libs/Galacticraft-1.12.2-4.0.2.283.jar'), target], text=True)
    source = (ROOT/f'src/main/java/galaxyspace/core/mixins/{mixin}.java').read_text()
    for selector in re.findall(r'@Inject\(method\s*=\s*(\{[^}]+\}|"[^"]+")', source):
        names = re.findall(r'"([^"]+)"', selector)
        assert any(re.search(r'\b' + re.escape(name) + r'\(', methods) for name in names), \
            f'{mixin}: no production GC method matches {names}'
print('Mixin targets in bundled GC jar: PASS')
