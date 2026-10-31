from pathlib import Path
import subprocess,sys
root=Path(__file__).resolve().parent
runtime,addon,work=map(Path,sys.argv[1:]);work.mkdir(exist_ok=True)
java=Path('/Users/hyberiid/Library/Java/JavaVirtualMachines/corretto-1.8.0_442/Contents/Home/bin')
paths=(runtime/'classpath-runtime.txt').read_text().split(':')
paths=[p for p in paths if 'GSLaunchCompat-' not in p]
cp=':'.join([str(work),str(addon)]+paths)
subprocess.run([str(java/'javac'),'-proc:none','-cp',cp,'-d',str(work),str(root/'TelemetryProbe.java'),str(root/'TelemetryTweaker.java')],check=True)
for side in ('server','client'):
 command=[str(java/'java'),'-Djava.awt.headless=true','-Dprobe.client='+str(side=='client').lower(),'-Dmixin.debug.export=true','-cp',cp,'net.minecraft.launchwrapper.Launch','--tweakClass','bootstrap.TelemetryTweaker','--tweakClass','org.spongepowered.asm.launch.MixinTweaker']
 with (work/(side+'.log')).open('w') as log:r=subprocess.run(command,cwd=work,stdout=log,stderr=subprocess.STDOUT,timeout=60)
 print(side,r.returncode)
 assert r.returncode==0,(work/(side+'.log')).read_text()[-6000:]
