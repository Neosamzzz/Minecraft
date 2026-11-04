"""Build NewEarth and export its current mod package."""
import json
import io
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile


def build(ROOT, run_gradle, env, offline):
    env = dict(env)
    modern_home = env.get('NEWEARTH_JAVA_HOME')
    if not modern_home and os.name == 'nt' and env.get('APPDATA'):
        candidate = Path(env['APPDATA']) / '.minecraft/runtime/java-runtime-epsilon'
        if (candidate / 'bin/javac.exe').is_file():
            modern_home = str(candidate)
    if not modern_home:
        raise ValueError('NewEarth terrain requires JDK 25: set NEWEARTH_JAVA_HOME.')
    for tool in ('java', 'javac'):
        executable = Path(modern_home) / 'bin' / (tool + ('.exe' if os.name == 'nt' else ''))
        result = subprocess.run([str(executable), '-version'], capture_output=True, text=True, timeout=20)
        if result.returncode or not re.search(r'\b25(?:[.\s"-]|$)', result.stdout + result.stderr):
            raise ValueError('NEWEARTH_JAVA_HOME must contain JDK 25 java and javac.')
    env['NEWEARTH_JAVA_HOME'] = str(Path(modern_home).resolve())
    properties = (ROOT / 'project/NewEarth/gradle.properties').read_text(encoding='utf-8')
    match = re.search(r'^mod_version=([A-Za-z0-9][A-Za-z0-9._+-]*)$', properties, re.MULTILINE)
    if not match:
        raise ValueError('Invalid NewEarth mod_version.')
    version = match.group(1)
    entrypoint = (ROOT / 'project/NewEarth/src/main/java/newearth/NewEarth.java').read_text(encoding='utf-8')
    declared = re.search(r'public static final String VERSION = "([^"]+)";', entrypoint)
    if not declared or declared.group(1) != version:
        raise ValueError('NewEarth.VERSION must match gradle.properties mod_version.')
    run_gradle('project/NewEarth', 'build', env, offline, wrapper='dependencies/GalaxySpace')
    artifact = ROOT / 'project/NewEarth/build/libs' / ('NewEarth-' + version + '.jar')
    if not artifact.is_file():
        raise FileNotFoundError('Expected NewEarth artifact was not produced: {}'.format(artifact))
    with zipfile.ZipFile(artifact) as jar:
        metadata = json.loads(jar.read('mcmod.info').decode('utf-8'))
        if len(metadata) != 1 or metadata[0].get('modid') != 'newearth' or metadata[0].get('version') != version:
            raise ValueError('NewEarth metadata does not match the requested version.')
        if 'newearth/NewEarth.class' not in jar.namelist():
            raise ValueError('NewEarth artifact has no mod entry point.')
        with zipfile.ZipFile(io.BytesIO(jar.read('META-INF/newearth-terrain.jar'))) as terrain:
            core = terrain.read('newearth/terrain/TerrainEngine.class')
            if int.from_bytes(core[6:8], 'big') != 69:
                raise ValueError('Terrain engine must be compiled for Java 25.')
            terrain.read('newearth/terrain/default-preset.json')
            terrain.read('newearth/terrain/biomes.png')
            terrain.read('newearth/terrain/biomes.txt')
    output = ROOT / 'build/libs' / artifact.name
    output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(str(artifact), str(output))
    print('Built: {}'.format(output))
    preset = ROOT / 'project/NewEarth/dimension/biome-bundle'
    bundle = ROOT / 'build/libs' / ('NewEarth-BiomeBundle-' + version + '.zip')
    with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.write(output, 'mods/' + output.name)
        for options in sorted((ROOT / 'project/NewEarth/dimension/client-config').glob('*.txt')):
            archive.write(options, 'shaderpacks/' + options.name)
        for name in ('ExtraPlanets.cfg', 'GalaxySpace/dimensions.conf'):
            archive.write(ROOT / 'config' / name, 'config/' + name)
        for config in sorted((preset / 'WorldBiomes').glob('*.bc')):
            archive.write(config, 'mods/OpenTerrainGenerator/worlds/Biome Bundle/WorldBiomes/' + config.name)
    print('Biome Bundle integration package (requires OTG 9.9 and Biome Bundle 6.1): {}'.format(bundle))
