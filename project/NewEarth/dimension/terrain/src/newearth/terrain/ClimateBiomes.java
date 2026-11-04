package newearth.terrain;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/** Nearest climate hypercube lookup over the original 1.20.1 overworld parameter list. */
public final class ClimateBiomes {
    private static final ClimateBiomes INSTANCE = new ClimateBiomes();
    private final String[] names;
    private final Entry[] entries;
    private final Branch root;

    private record Entry(int index, String name, int[] bounds, long offset) {}
    private static final class Search {
        final long[] point;
        long distance = Long.MAX_VALUE;
        Entry entry;
        Search(long[] point) { this.point=point; }
    }

    private ClimateBiomes() {
        var source=ClimateBiomes.class.getResourceAsStream("/newearth/terrain/biome-climate.bin");
        if(source==null)throw new IllegalStateException("Missing vanilla climate parameters");
        try(var input=new DataInputStream(source)) {
            if(input.readInt()!=0x4E454243)throw new IOException("Unknown climate table format");
            entries=new Entry[input.readInt()];names=new String[input.readInt()];
            for(int i=0;i<names.length;i++)names[i]=input.readUTF();
            for(int i=0;i<entries.length;i++) {
                String name=names[input.readUnsignedByte()];int[] bounds=new int[12];
                for(int j=0;j<12;j++)bounds[j]=input.readInt();
                entries[i]=new Entry(i,name,bounds,input.readInt());
            }
        }catch(IOException error){throw new IllegalStateException(error);}
        root=new Branch(entries.clone());
    }

    public static String at(float temperature,float moisture,float continent,float erosion,float depth,float weirdness) {
        Search search=new Search(new long[]{quantize(temperature),quantize(moisture),quantize(continent),quantize(erosion),quantize(depth),quantize(weirdness)});
        INSTANCE.root.find(search);return search.entry.name;
    }
    private static long quantize(float value){return (long)(value*10000F);}

    private static long distance(long[] point,int[] bounds,long stop) {
        long result=0;
        for(int i=0;i<6;i++) {
            long d=Math.max(bounds[i*2]-point[i],point[i]-bounds[i*2+1]);
            if(d>0)result+=d*d;
            if(result>stop)return result;
        }
        return result;
    }

    private static final class Branch {
        final int[] bounds=new int[12];
        final Branch left,right;
        final Entry[] leaves;
        Branch(Entry[] values) {
            for(int i=0;i<6;i++){bounds[i*2]=Integer.MAX_VALUE;bounds[i*2+1]=Integer.MIN_VALUE;}
            for(Entry entry:values)for(int i=0;i<6;i++) {
                bounds[i*2]=Math.min(bounds[i*2],entry.bounds[i*2]);
                bounds[i*2+1]=Math.max(bounds[i*2+1],entry.bounds[i*2+1]);
            }
            if(values.length<=12){leaves=values;left=null;right=null;return;}
            int axis=0;long spread=-1;
            for(int i=0;i<6;i++) {
                long min=Long.MAX_VALUE,max=Long.MIN_VALUE;
                for(Entry e:values){long center=(long)e.bounds[i*2]+e.bounds[i*2+1];min=Math.min(min,center);max=Math.max(max,center);}
                if(max-min>spread){spread=max-min;axis=i;}
            }
            final int dimension=axis;
            Arrays.sort(values,Comparator.comparingLong(e->(long)e.bounds[dimension*2]+e.bounds[dimension*2+1]));
            int middle=values.length/2;
            left=new Branch(Arrays.copyOfRange(values,0,middle));right=new Branch(Arrays.copyOfRange(values,middle,values.length));leaves=null;
        }
        void find(Search search) {
            if(distance(search.point,bounds,search.distance)>search.distance)return;
            if(leaves!=null) {
                for(Entry entry:leaves) {
                    long d=distance(search.point,entry.bounds,search.distance)+entry.offset*entry.offset;
                    if(d<search.distance || (d==search.distance && (search.entry==null || entry.index<search.entry.index))) {
                        search.distance=d;search.entry=entry;
                    }
                }
                return;
            }
            if(distance(search.point,left.bounds,search.distance)<=distance(search.point,right.bounds,search.distance)){left.find(search);right.find(search);}
            else {right.find(search);left.find(search);}
        }
    }
}
