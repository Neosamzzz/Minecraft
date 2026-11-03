package bootstrap;
import java.io.File;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
public class PublicControllerTweaker implements ITweaker {
 public void acceptOptions(List<String> args,File gameDir,File assetsDir,String profile){}
 public void injectIntoClassLoader(LaunchClassLoader loader){
  org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setSide(org.spongepowered.asm.mixin.MixinEnvironment.Side.CLIENT);
  if(Boolean.getBoolean("probe.mixins"))org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.gslaunchcompat.json");
 }
 public String getLaunchTarget(){return "diagnostic.PublicControllerProbe";}
 public String[] getLaunchArguments(){return new String[0];}
}
