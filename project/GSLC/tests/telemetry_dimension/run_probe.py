"""Run isolated GC telemetry probes; this does not cover full Forge startup."""
from pathlib import Path
import subprocess
import sys

from probe_runtime import arguments, prepare

root = Path(__file__).resolve().parent
java, javac, cp, work = prepare(*arguments('telemetry'))
subprocess.run([javac, '-proc:none', '-cp', cp, '-d', str(work),
                str(root / 'TelemetryProbe.java'), str(root / 'TelemetryTweaker.java')], check=True)
for side in ('server', 'client'):
    command = [java, '-Djava.awt.headless=true',
               '-Dprobe.client=' + str(side == 'client').lower(),
               '-Dmixin.debug.export=true', '-cp', cp,
               'net.minecraft.launchwrapper.Launch',
               '--tweakClass', 'bootstrap.TelemetryTweaker',
               '--tweakClass', 'org.spongepowered.asm.launch.MixinTweaker']
    with (work / (side + '.log')).open('w') as log:
        result = subprocess.run(command, cwd=work, stdout=log,
                                stderr=subprocess.STDOUT, timeout=60)
    print(side, result.returncode)
    assert result.returncode == 0, (work / (side + '.log')).read_text()[-6000:]
