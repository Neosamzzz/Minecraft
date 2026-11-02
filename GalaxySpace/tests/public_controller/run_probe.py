from pathlib import Path
import subprocess
import sys
root=Path(__file__).resolve().parent
sys.path.insert(0, str(root.parent / 'telemetry_dimension'))
from probe_runtime import arguments, prepare
java, javac, cp, work = prepare(*arguments('public-controller'))
subprocess.run([javac,'-proc:none','-cp',cp,'-d',str(work),str(root/'PublicControllerProbe.java'),str(root/'PublicControllerTweaker.java')],check=True)
for mode in ['baseline','patched']:
 cmd=[java,'-Djava.awt.headless=true','-Dprobe.mixins='+str(mode=='patched').lower(),'-Dmixin.debug.export=true','-cp',cp,'net.minecraft.launchwrapper.Launch','--tweakClass','bootstrap.PublicControllerTweaker','--tweakClass','org.spongepowered.asm.launch.MixinTweaker']
 with (work/(mode+'.log')).open('w') as f:r=subprocess.run(cmd,cwd=work,stdout=f,stderr=subprocess.STDOUT,timeout=45)
 print(mode,r.returncode)
 assert r.returncode==0,(work/(mode+'.log')).read_text()[-5000:]
