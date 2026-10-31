"""Run isolated GC/Minecraft map probes; this does not cover full Forge startup."""
from pathlib import Path
import subprocess
import sys

from probe_runtime import prepare

java, javac, cp, work = prepare(*sys.argv[1:])
sources = Path(__file__).resolve().parent
subprocess.run([javac, '-proc:none', '-cp', cp, '-d', str(work),
                str(sources / 'SurfaceMapProbe.java'), str(sources / 'TelemetryTweaker.java')], check=True)
with (work / 'surface.log').open('w') as log:
    result = subprocess.run([java, '-Djava.awt.headless=true', '-Dprobe.client=true',
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
