package newearth.terrain;
import newearth.terrain.world.worldgen.*;import newearth.terrain.world.worldgen.cell.*;
import newearth.terrain.data.worldgen.preset.*;import newearth.terrain.data.worldgen.preset.settings.*;
public final class TerrainReferenceCheck {public static void main(String[] args)throws Exception{
 var preset=new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(
 TerrainReferenceCheck.class.getResourceAsStream("/newearth/terrain/default-preset.json"),java.nio.charset.StandardCharsets.UTF_8),Preset.class);
 var registry=new NoiseRegistry();PresetNoiseData.bootstrap(preset,registry);
 for(int seed:new int[]{42,-20261103,1992175465})run(GeneratorContext.makeUncached(preset,registry,seed,3,1,1),seed);
 verifyDensity(args[0]);
 System.out.println("RTF terrain and final density reference checks passed");
 }

 static void verifyDensity(String path)throws Exception {
  newearth.terrain.TerrainEngine engine=null;
  newearth.terrain.density.DensityRouter router=null;long lastSeed=0;
  try(var in=new java.io.DataInputStream(new java.io.FileInputStream(path))) {
   if(in.readInt()!=0x4E45444E)throw new AssertionError("Unknown density fixture");
   int count=in.readInt();
   for(int i=0;i<count;i++) {
    long seed=in.readLong();int x=in.readInt(),y=in.readInt(),z=in.readInt();double expected=in.readDouble();
    if(engine==null || seed!=lastSeed) {
     if(engine!=null)engine.close();engine=new newearth.terrain.TerrainEngine(seed);
     router=new newearth.terrain.density.DensityRouter(seed,engine::field);lastSeed=seed;
    }
    double actual=router.sample(x,y,z);
    if(Math.abs(actual-expected)>1e-8 || (actual>0)!=(expected>0))throw new AssertionError("Final density mismatch at "+seed+":"+x+","+y+","+z+" expected "+expected+" got "+actual);
   }
  }finally{if(engine!=null)engine.close();}
 }
 // Generated with the installed unmodified RTF 0.0.6 JAR (SHA-256 e0fe886f45cf7e0964432c6c1a9ff4ea8d27631938586166376b815836a0eab2).
 static final java.util.Set<String> EXPECTED=new java.util.HashSet<>(java.util.Arrays.asList(
"42 raw 9bcdffb4467970a9d0349026332450013b0c88ba99699ea5ba796cf59973ebdf",
"42 tile 0,0 0e492b70f7f6ea5cebd23e680912dcb5b57237daff695ae184d302a6616e6027",
"42 tile -1,0 2bd3f1991ed2f136076b51fb927c2e92f5cfc399bf034662467657a70ba63a11",
"42 tile 12,-9 79c7e1db8a2c6b810e448fca92e5d03883f63f38455b2d72b6d8535851f5b6c1",
"42 tile 16,31 2b057c1680f62f1dea38d553b2e1c687fb0c3c5a7fe73490470ebf1f3bea4b3b",
"-20261103 raw 248af79771983c98a2465b07c973dd6d15bc7c37665a1f2b09ef6a2d9df0199d",
"-20261103 tile 0,0 a8484c84fa8adf6e60863ba94b5882e9fc9c841df1f81e22ddf745ae77adbe73",
"-20261103 tile -1,0 b92063fa601d699de86cc0f7b422385ae329f1bf6a07d53aff34d096690b418d",
"-20261103 tile 12,-9 c3fc14673d5bf334db66dd29189fb64d1e4bac2aa532870ec26f42cb6b4447f8",
"-20261103 tile -6,30 2ad5bbe96cb759bee33aaab1f2505e665cf9c105b1cb047a1003885a8ba6918d",
"1992175465 raw 5bfaeeabc4fc16c4e2b081640ee5ecff1892c599252f59321fd45c949a17b912",
"1992175465 tile 0,0 c800772d76bc2e70569bfc5a43c36b3d4fb067ea582f3369aeb0afd817772607",
"1992175465 tile -1,0 c0eb8fdf2a81e33c90adb16456fa47514535e1dea2d40ab9b2d7873d58d0c9fe",
"1992175465 tile 12,-9 2c29697dc7ffff6d77db41f2dfe9f81767d4a021b7f7ba4cb5f00d8cf1da3319",
"1992175465 tile 30,-6 6b876e5699aa7bab413fe6ffc7896a39e77f76676d87b3c4821e22c5323b5a8f"));
 static void verify(String result){if(!EXPECTED.contains(result))throw new AssertionError("RTF mismatch: "+result);}
 static java.security.MessageDigest digest;
 static void value(float f){int v=Float.floatToIntBits(f);for(int shift=24;shift>=0;shift-=8)digest.update((byte)(v>>shift));}
 static void sample(Cell c){
  value(c.height);value(c.gradient);value(c.continentEdge);value(c.riverMask);
  value(c.temperature);value(c.moisture);value(c.erosion);value(c.weirdness);
  digest.update(c.terrain.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);
  digest.update(c.biome.name().getBytes(java.nio.charset.StandardCharsets.UTF_8));digest.update((byte)0);
 }
 static String hash(){StringBuilder s=new StringBuilder();for(byte b:digest.digest())s.append(String.format("%02x",b&255));return s.toString();}
 static void run(GeneratorContext c,int seed)throws Exception{
  digest=java.security.MessageDigest.getInstance("SHA-256");float max=0;int mx=0,mz=0;
  for(int z=-4096;z<=4096;z+=64)for(int x=-4096;x<=4096;x+=64){Cell cell=new Cell();c.generator.getHeightmap().apply(cell,x,z,true);sample(cell);if(cell.height>max){max=cell.height;mx=x;mz=z;}}
  verify(seed+" raw "+hash());
  for(int[] at:new int[][]{{0,0},{-1,0},{12,-9},{mx>>7,mz>>7}}){
   var tile=c.generator.generate(at[0],at[1]).join();digest=java.security.MessageDigest.getInstance("SHA-256");
   for(int z=0;z<128;z+=4)for(int x=0;x<128;x+=4)sample(tile.lookup(x,z));
   verify(seed+" tile "+at[0]+","+at[1]+" "+hash());tile.close();
  }
 }
}
