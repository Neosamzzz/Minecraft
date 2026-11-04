package newearth.terrain.density;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Evaluates the exported RTF Default density graph, including its vanilla cave functions. */
public final class DensityRouter {
    @FunctionalInterface public interface Fields { double read(String name, int x, int z); }
    @FunctionalInterface private interface Expression { double at(int x, int y, int z); }
    private final JsonObject document, functions;
    private final Fields fields;
    private final long seed;
    private final Map<String, Node> references = new HashMap<>();
    private final Map<String, NormalNoise> noises = new HashMap<>();
    private final List<Map<Position, Double>> interpolationCaches = new ArrayList<>();
    private final Node root;

    public DensityRouter(long seed, Fields fields) {
        this.seed = seed; this.fields = fields;
        var stream = DensityRouter.class.getResourceAsStream("/newearth/terrain/density-router.json");
        if (stream == null) throw new IllegalStateException("Missing RTF density router");
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            document = new Gson().fromJson(reader, JsonObject.class);
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        functions = document.getAsJsonObject("functions");
        root = compile(document.get("root"));
    }

    public double sample(int x, int y, int z) { return root.at(x,y,z); }
    public void beginChunk() { interpolationCaches.forEach(Map::clear); }
    public NormalNoise noise(String key) {
        return noises.computeIfAbsent(key, name -> {
            JsonObject parameters = document.getAsJsonObject("noises").getAsJsonObject(name);
            if (parameters == null) throw new IllegalArgumentException("Unknown noise " + name);
            JsonArray array = parameters.getAsJsonArray("amplitudes");
            double[] amplitudes = new double[array.size()];
            for (int i=0;i<amplitudes.length;i++) amplitudes[i]=array.get(i).getAsDouble();
            return new NormalNoise(seed,name,parameters.get("firstOctave").getAsInt(),amplitudes);
        });
    }

    private static final class Node {
        private final Expression expression;
        private boolean valid;
        private int lastX,lastY,lastZ;
        private double value;
        Node(Expression expression) { this.expression=expression; }
        double at(int x,int y,int z) {
            if (!valid || lastX!=x || lastY!=y || lastZ!=z) {
                double result=expression.at(x,y,z);
                value=result;lastX=x;lastY=y;lastZ=z;valid=true;
            }
            return value;
        }
    }

    private Node compile(JsonElement value) {
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive=value.getAsJsonPrimitive();
            if (primitive.isNumber()) { double number=value.getAsDouble();return new Node((x,y,z)->number); }
            String key=value.getAsString();
            Node found=references.get(key);
            if (found!=null) return found;
            JsonElement definition=functions.get(key);
            if (definition==null) throw new IllegalArgumentException("Unknown density function "+key);
            Node compiled=compile(definition);references.put(key,compiled);return compiled;
        }
        JsonObject object=value.getAsJsonObject();
        String type=object.get("type").getAsString();
        if (type.equals("reterraforged:cell")) {
            String name=object.get("field").getAsString();
            return new Node((x,y,z)->fields.read(name,x,z));
        }
        if (type.equals("reterraforged:clamp_to_nearest_unit")) {
            Node input=compile(object.get("function"));int resolution=object.get("resolution").getAsInt();
            return new Node((x,y,z)->(float)((int)(input.at(x,y,z)*resolution)+1)/(float)resolution);
        }
        String name=type.substring(type.indexOf(':')+1);
        switch (name) {
            case "add", "mul", "min", "max": {
                Node a=compile(object.get("argument1")), b=compile(object.get("argument2"));
                return new Node((x,y,z)-> {
                    double av=a.at(x,y,z);
                    if (name.equals("mul") && av==0) return 0;
                    double bv=b.at(x,y,z);
                    return switch(name) {case "add"->av+bv;case "mul"->av*bv;case "min"->Math.min(av,bv);default->Math.max(av,bv);};
                });
            }
            case "noise": {
                NormalNoise noise=noise(object.get("noise").getAsString());
                double horizontal=object.get("xz_scale").getAsDouble(),vertical=object.get("y_scale").getAsDouble();
                return new Node((x,y,z)->noise.sample(x*horizontal,y*vertical,z*horizontal));
            }
            case "weird_scaled_sampler": {
                NormalNoise noise=noise(object.get("noise").getAsString());Node input=compile(object.get("input"));
                String mapper=object.get("rarity_value_mapper").getAsString();
                return new Node((x,y,z)-> {
                    double v=input.at(x,y,z);
                    double rarity=mapper.equals("type_2") ? (v<-.75?.5:v<-.5?.75:v<.5?1:v<.75?2:3)
                            : (v<-.5?.75:v<0?1:v<.5?1.5:2);
                    return rarity*Math.abs(noise.sample(x/rarity,y/rarity,z/rarity));
                });
            }
            case "y_clamped_gradient": {
                int from=object.get("from_y").getAsInt(),to=object.get("to_y").getAsInt();
                double a=object.get("from_value").getAsDouble(),b=object.get("to_value").getAsDouble();
                return new Node((x,y,z)->NormalNoise.lerp(clamp((y-from)/(double)(to-from),0,1),a,b));
            }
            case "range_choice": {
                Node input=compile(object.get("input")),inside=compile(object.get("when_in_range")),outside=compile(object.get("when_out_of_range"));
                double min=object.get("min_inclusive").getAsDouble(),max=object.get("max_exclusive").getAsDouble();
                return new Node((x,y,z)->{double n=input.at(x,y,z);return (n>=min && n<max?inside:outside).at(x,y,z);});
            }
            case "clamp": {
                Node input=compile(object.get("input"));double min=object.get("min").getAsDouble(),max=object.get("max").getAsDouble();
                return new Node((x,y,z)->clamp(input.at(x,y,z),min,max));
            }
            case "spline": return spline(object.get("spline"));
            case "blend_alpha": return new Node((x,y,z)->1);
            case "flat_cache": {Node input=compile(object.get("argument"));return new Node((x,y,z)->input.at(x&~3,0,z&~3));}
            case "cache_2d": {Node input=compile(object.get("argument"));return new Node((x,y,z)->input.at(x,0,z));}
            case "cache_once": return compile(object.get("argument"));
            case "interpolated": return interpolate(compile(object.get("argument")));
            case "abs", "square", "cube", "half_negative", "quarter_negative", "squeeze": {
                Node input=compile(object.get("argument"));
                return new Node((x,y,z)-> {
                    double n=input.at(x,y,z);
                    return switch(name) {
                        case "abs"->Math.abs(n);case "square"->n*n;case "cube"->n*n*n;
                        case "half_negative"->n>0?n:n*.5;case "quarter_negative"->n>0?n:n*.25;
                        default->{double v=clamp(n,-1,1);yield v/2-v*v*v/24;}
                    };
                });
            }
            default: throw new IllegalArgumentException("Unsupported density node "+type);
        }
    }

    private record Position(int x,int y,int z) {}
    private Node interpolate(Node input) {
        Map<Position,Double> cache=new HashMap<>();interpolationCaches.add(cache);
        return new Node((x,y,z)-> {
            int x0=x&~3,y0=y&~7,z0=z&~3;
            double fx=(x-x0)*.25,fy=(y-y0)*.125,fz=(z-z0)*.25;
            double[] vertical=new double[4];
            for(int dz=0;dz<2;dz++)for(int dx=0;dx<2;dx++){
                int px=x0+dx*4,pz=z0+dz*4;
                double a=cache.computeIfAbsent(new Position(px,y0,pz),p->input.at(p.x,p.y,p.z));
                double b=cache.computeIfAbsent(new Position(px,y0+8,pz),p->input.at(p.x,p.y,p.z));
                vertical[dz*2+dx]=NormalNoise.lerp(fy,a,b);
            }
            return NormalNoise.lerp(fz,NormalNoise.lerp(fx,vertical[0],vertical[1]),NormalNoise.lerp(fx,vertical[2],vertical[3]));
        });
    }

    private Node spline(JsonElement element) {
        if (element.isJsonPrimitive()) {float value=element.getAsFloat();return new Node((x,y,z)->value);}
        JsonObject object=element.getAsJsonObject();Node coordinate=compile(object.get("coordinate"));
        JsonArray points=object.getAsJsonArray("points");int size=points.size();
        float[] locations=new float[size],derivatives=new float[size];Node[] values=new Node[size];
        for(int i=0;i<size;i++) {
            JsonObject point=points.get(i).getAsJsonObject();locations[i]=point.get("location").getAsFloat();
            derivatives[i]=point.get("derivative").getAsFloat();values[i]=spline(point.get("value"));
        }
        return new Node((x,y,z)-> {
            float c=(float)coordinate.at(x,y,z);int i=0;while(i<size && c>=locations[i])i++;i--;
            if(i<0)return (float)values[0].at(x,y,z)+derivatives[0]*(c-locations[0]);
            if(i==size-1)return (float)values[i].at(x,y,z)+derivatives[i]*(c-locations[i]);
            float width=locations[i+1]-locations[i],t=(c-locations[i])/width;
            float a=(float)values[i].at(x,y,z),b=(float)values[i+1].at(x,y,z),difference=b-a;
            float left=derivatives[i]*width-difference,right=-derivatives[i+1]*width+difference;
            return (a+t*(b-a))+t*(1-t)*(left+t*(right-left));
        });
    }
    private static double clamp(double n,double min,double max) {return Math.max(min,Math.min(max,n));}
}
