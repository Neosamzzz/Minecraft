package newearth.terrain.density;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Double-Perlin noise with the 1.20 positional seed and octave construction. */
public final class NormalNoise {
    private final Octaves first, second;
    private final double scale;

    public NormalNoise(long worldSeed, String key, int firstOctave, double[] amplitudes) {
        Random world = new Random(worldSeed);
        long lo = world.next(), hi = world.next();
        Random source = keyed(lo, hi, key);
        first = new Octaves(source, firstOctave, amplitudes);
        second = new Octaves(source, firstOctave, amplitudes);
        int min = amplitudes.length, max = -1;
        for (int i = 0; i < amplitudes.length; i++) if (amplitudes[i] != 0) {
            min = Math.min(min, i); max = i;
        }
        scale = (1.0 / 6.0) / (0.1 * (1 + 1.0 / (max - min + 1)));
    }

    public double sample(double x, double y, double z) {
        double stretch = 1.0181268882175227;
        return (first.sample(x, y, z) + second.sample(x * stretch, y * stretch, z * stretch)) * scale;
    }

    private static Random keyed(long lo, long hi, String key) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(key.getBytes(StandardCharsets.UTF_8));
            ByteBuffer bytes = ByteBuffer.wrap(hash);
            return new Random(lo ^ bytes.getLong(), hi ^ bytes.getLong());
        } catch (NoSuchAlgorithmException error) { throw new AssertionError(error); }
    }

    private static final class Random {
        private long lo, hi;
        Random(long seed) {
            long a = seed ^ 0x6A09E667F3BCC909L;
            lo = mix(a); hi = mix(a + 0x9E3779B97F4A7C15L);
        }
        Random(long lo, long hi) {
            this.lo = lo; this.hi = hi;
            if ((lo | hi) == 0) { this.lo = 0x9E3779B97F4A7C15L; this.hi = 0x6A09E667F3BCC909L; }
        }
        private static long mix(long n) {
            n = (n ^ n >>> 30) * 0xBF58476D1CE4E5B9L;
            n = (n ^ n >>> 27) * 0x94D049BB133111EBL;
            return n ^ n >>> 31;
        }
        long next() {
            long a = lo, b = hi;
            long result = Long.rotateLeft(a + b, 17) + a;
            b ^= a;
            lo = Long.rotateLeft(a, 49) ^ b ^ b << 21;
            hi = Long.rotateLeft(b, 28);
            return result;
        }
        double unit() { return (next() >>> 11) * 0x1.0p-53; }
        int bounded(int bound) {
            long product = Integer.toUnsignedLong((int) next()) * bound;
            long lower = product & 0xFFFFFFFFL;
            if (lower < bound) {
                int reject = Integer.remainderUnsigned(-bound, bound);
                while (lower < reject) {
                    product = Integer.toUnsignedLong((int) next()) * bound;
                    lower = product & 0xFFFFFFFFL;
                }
            }
            return (int) (product >>> 32);
        }
    }

    private static final class Octaves {
        private final Improved[] layers;
        private final double[] amplitudes;
        private final double frequency, valueScale;
        Octaves(Random random, int octave, double[] amplitudes) {
            this.amplitudes = amplitudes.clone();
            layers = new Improved[amplitudes.length];
            long lo = random.next(), hi = random.next();
            for (int i = 0; i < layers.length; i++) if (amplitudes[i] != 0)
                layers[i] = new Improved(keyed(lo, hi, "octave_" + (octave + i)));
            frequency = Math.pow(2, octave);
            valueScale = Math.pow(2, layers.length - 1) / (Math.pow(2, layers.length) - 1);
        }
        double sample(double x, double y, double z) {
            double sum = 0, f = frequency, v = valueScale;
            for (int i = 0; i < layers.length; i++) {
                if (layers[i] != null) sum += amplitudes[i] * layers[i].sample(wrap(x*f), wrap(y*f), wrap(z*f)) * v;
                f *= 2; v /= 2;
            }
            return sum;
        }
        private static double wrap(double x) { return x - Math.floor(x / 33554432.0 + 0.5) * 33554432.0; }
    }

    private static final class Improved {
        private final int[] permutation = new int[256];
        private final double ox, oy, oz;
        Improved(Random random) {
            ox = random.unit()*256; oy = random.unit()*256; oz = random.unit()*256;
            for (int i=0;i<256;i++) permutation[i]=i;
            for (int i=0;i<256;i++) {
                int j=i+random.bounded(256-i), old=permutation[i];
                permutation[i]=permutation[j];permutation[j]=old;
            }
        }
        private int p(int i) { return permutation[i & 255]; }
        double sample(double x, double y, double z) {
            x+=ox;y+=oy;z+=oz;
            int ix=(int)Math.floor(x), iy=(int)Math.floor(y), iz=(int)Math.floor(z);
            double fx=x-ix,fy=y-iy,fz=z-iz;
            int a=p(ix),b=p(ix+1),aa=p(a+iy),ab=p(a+iy+1),ba=p(b+iy),bb=p(b+iy+1);
            double x0=lerp(fade(fx),grad(p(aa+iz),fx,fy,fz),grad(p(ba+iz),fx-1,fy,fz));
            double x1=lerp(fade(fx),grad(p(ab+iz),fx,fy-1,fz),grad(p(bb+iz),fx-1,fy-1,fz));
            double x2=lerp(fade(fx),grad(p(aa+iz+1),fx,fy,fz-1),grad(p(ba+iz+1),fx-1,fy,fz-1));
            double x3=lerp(fade(fx),grad(p(ab+iz+1),fx,fy-1,fz-1),grad(p(bb+iz+1),fx-1,fy-1,fz-1));
            return lerp(fade(fz),lerp(fade(fy),x0,x1),lerp(fade(fy),x2,x3));
        }
        private static double fade(double n) { return n*n*n*(n*(n*6-15)+10); }
        private static double grad(int hash,double x,double y,double z) {
            int h=hash & 15; double u=h<8?x:y, v=h<4?y:(h==12||h==14?x:z);
            return ((h&1)==0?u:-u)+((h&2)==0?v:-v);
        }
    }
    static double lerp(double t,double a,double b) { return a+t*(b-a); }
}
