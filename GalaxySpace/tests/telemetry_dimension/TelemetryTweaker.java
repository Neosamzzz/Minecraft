package bootstrap;
import java.io.File;import java.util.List;
import net.minecraft.launchwrapper.*;
public class TelemetryTweaker implements ITweaker {
 public void acceptOptions(List<String>a,File b,File c,String d){}
 public void injectIntoClassLoader(LaunchClassLoader l){
  org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setSide(Boolean.getBoolean("probe.client")?org.spongepowered.asm.mixin.MixinEnvironment.Side.CLIENT:org.spongepowered.asm.mixin.MixinEnvironment.Side.SERVER);
  org.spongepowered.asm.mixin.Mixins.addConfiguration("mixins.gslaunchcompat.json");
 }
 public String getLaunchTarget(){return System.getProperty("probe.target","diagnostic.TelemetryProbe");}
 public String[] getLaunchArguments(){return new String[0];}
}
