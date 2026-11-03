import java.lang.reflect.Method;

public class ScreenClockTest {
    public static void main(String[] args) throws Exception {
        Method format = Class.forName("galaxyspace.core.telemetry.ScreenClock").getMethod("format", long.class);
        long[] times = {0, 6000, 12000, 18000, 24000, -6000, 500, 23999};
        String[] expected = {"06:00", "12:00", "18:00", "00:00", "06:00", "00:00", "06:30", "05:59"};
        for (int i = 0; i < times.length; i++) {
            String text = (String) format.invoke(null, times[i]);
            if (!text.equals(expected[i])) throw new AssertionError(times[i] + " => " + text);
            if (!text.matches("\\d{2}:\\d{2}")) throw new AssertionError("Seconds were retained");
        }
        System.out.println("CLOCK_PASS 8 cases; vanilla 24000-tick day; HH:mm only");
    }
}
