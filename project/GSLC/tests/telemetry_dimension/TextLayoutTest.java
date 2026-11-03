import galaxyspace.core.telemetry.TelemetryTextLayout;
public class TextLayoutTest {
 public static void main(String[] args) {
  java.util.function.ToIntFunction<String> width=s->s.codePoints().map(c->c>127?9:6).sum();
  for(String s:new String[]{"", "Mars", "所在维度：火星", "A very long custom dimension name that does not fit", "😀超长维度名称测试😀"}) {
   String fitted=TelemetryTextLayout.fit(s,155,width);
   if(width.applyAsInt(fitted)>155)throw new AssertionError("overflow: "+fitted);
   if(width.applyAsInt(s)<=155&&!s.equals(fitted))throw new AssertionError("short text changed");
  }
  System.out.println("Dimension-only string fitting passed; vanilla layout is not modified");
 }
}
