from pathlib import Path
import subprocess
import sys
root=Path(__file__).resolve().parent
runtime=Path(sys.argv[1]); addon=Path(sys.argv[2]); work=Path(sys.argv[3]); work.mkdir(exist_ok=True)
java=Path('/Users/hyberiid/Library/Java/JavaVirtualMachines/corretto-1.8.0_442/Contents/Home/bin')
paths=(runtime/'classpath-runtime.txt').read_text().split(':')
paths=[p for p in paths if 'GSLaunchCompat-' not in p and not p.endswith('/runtime-probe/classes')]
cp=':'.join([str(work),str(addon)]+paths)
subprocess.run([str(java/'javac'),'-proc:none','-cp',cp,'-d',str(work),str(root/'PublicControllerProbe.java'),str(root/'PublicControllerTweaker.java')],check=True)
for mode in ['baseline','patched']:
 cmd=[str(java/'java'),'-Djava.awt.headless=true','-Dprobe.mixins='+str(mode=='patched').lower(),'-Dmixin.debug.export=true','-cp',cp,'net.minecraft.launchwrapper.Launch','--tweakClass','bootstrap.PublicControllerTweaker','--tweakClass','org.spongepowered.asm.launch.MixinTweaker']
 with (work/(mode+'.log')).open('w') as f:r=subprocess.run(cmd,cwd=work,stdout=f,stderr=subprocess.STDOUT,timeout=45)
 print(mode,r.returncode)
 assert r.returncode==0,(work/(mode+'.log')).read_text()[-5000:]
