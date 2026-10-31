"""Run real GC/Minecraft surface-map integration in a headless client loader."""
from pathlib import Path
import subprocess
import sys

runtime, addon, work = (Path(value).resolve() for value in sys.argv[1:])
work.mkdir(exist_ok=True)
java = Path('/Users/hyberiid/Library/Java/JavaVirtualMachines/corretto-1.8.0_442/Contents/Home/bin')
sources = Path(__file__).resolve().parent
paths = [path for path in (runtime / 'classpath-runtime.txt').read_text().strip().split(':')
         if 'GSLaunchCompat-' not in path]
cp = ':'.join([str(work), str(addon)] + paths)
subprocess.run([str(java / 'javac'), '-proc:none', '-cp', cp, '-d', str(work),
                str(sources / 'SurfaceMapProbe.java'), str(sources / 'TelemetryTweaker.java')], check=True)
with (work / 'surface.log').open('w') as log:
    result = subprocess.run([str(java / 'java'), '-Djava.awt.headless=true', '-Dprobe.client=true',
                             '-Dprobe.target=diagnostic.SurfaceMapProbe', '-Dmixin.debug.export=true',
                             '-cp', cp, 'net.minecraft.launchwrapper.Launch',
                             '--tweakClass', 'bootstrap.TelemetryTweaker',
                             '--tweakClass', 'org.spongepowered.asm.launch.MixinTweaker'],
                            cwd=work, stdout=log, stderr=subprocess.STDOUT, timeout=60)
output = (work / 'surface.log').read_text()
if result.returncode:
    print(output[-7000:])
    raise SystemExit(result.returncode)
assert 'SURFACE_MAP_PASS' in output
print(next(line for line in output.splitlines() if 'SURFACE_MAP_PASS' in line))
