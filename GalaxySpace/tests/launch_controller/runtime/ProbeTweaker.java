package bootstrap;
import java.io.File;
import java.util.List;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
public class ProbeTweaker implements ITweaker {
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) { }
    public void injectIntoClassLoader(LaunchClassLoader loader) {
        if (Boolean.getBoolean("probe.mixins")) {
            org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setSide(org.spongepowered.asm.mixin.MixinEnvironment.Side.SERVER);
            org.spongepowered.asm.mixin.Mixins.addConfiguration(System.getProperty("probe.config", "mixins.galaxyspace.json"));
        }
    }
    public String getLaunchTarget() { return "diagnostic.BindingProbe"; }
    public String[] getLaunchArguments() { return new String[0]; }
}
