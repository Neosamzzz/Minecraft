import java.io.File;
import java.util.Arrays;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.client.FMLFileResourcePack;
public class LanguagePackProbe {
 static void check(File jar,boolean expectChinese)throws Exception {
  ModMetadata metadata=new ModMetadata();metadata.modId="gslaunchtelemetry";metadata.name="Telemetry";
  DummyModContainer container=new DummyModContainer(metadata){@Override public File getSource(){return jar;}};
  IResourcePack pack=new FMLFileResourcePack(container);
  MetadataSerializer serializer=new MetadataSerializer();serializer.func_110504_a(new PackMetadataSectionSerializer(),PackMetadataSection.class);
  PackMetadataSection section=pack.func_135058_a(serializer,"pack");
  int format=section.func_110462_b();if(format==2)pack=new LegacyV2Adapter(pack);
  SimpleReloadableResourceManager manager=new SimpleReloadableResourceManager(serializer);manager.func_110541_a(Arrays.asList(pack));
  net.minecraft.client.resources.Locale locale=new net.minecraft.client.resources.Locale();locale.func_135022_a(manager,Arrays.asList("en_us","zh_cn"));
  String result=locale.func_135023_a("gslc.dimension.line",new Object[]{"火星"});
  boolean chinese="所在维度：火星".equals(result);
  System.out.println(jar.getName()+" pack_format="+format+" text="+result);
  if(chinese!=expectChinese)throw new AssertionError("Unexpected translation result");
 }
 public static void main(String[] args)throws Exception {check(new File(args[0]),false);check(new File(args[1]),true);}
}
