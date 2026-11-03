package galaxyspace.core.telemetry;

/** Preserve GC's original 24000-tick time convention, without seconds. */
public final class ScreenClock {
    private ScreenClock() { }

    public static String format(long time) {
        int minute = (int) ((Math.floorMod(time, 24000L) + 6000L) % 24000L * 1440L / 24000L);
        int hour = minute / 60;
        minute %= 60;
        return (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
    }
}
