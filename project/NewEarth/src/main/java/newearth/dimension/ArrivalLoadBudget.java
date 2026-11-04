package newearth.dimension;

/** Slow ramp-up, immediate backoff; shared policy for server ticks and client frame times. */
public final class ArrivalLoadBudget {
    public static final int MAX_LEVEL = 12;
    private final double healthyMs, overloadedMs;
    private double average;
    private int level;
    private long healthySince = -1, resumeAfter;

    public ArrivalLoadBudget(double healthyMs, double overloadedMs) {
        this.healthyMs = healthyMs; this.overloadedMs = overloadedMs;
    }

    public void sample(long now, double elapsedMs) {
        average = average == 0 ? elapsedMs : average * .8 + elapsedMs * .2;
        if (elapsedMs >= 120 || average >= overloadedMs) {
            level = Math.max(0, level / 2 - 1);
            resumeAfter = now + 1_500_000_000L;
            healthySince = -1;
        } else if (now >= resumeAfter && elapsedMs < healthyMs && average < healthyMs) {
            if (healthySince < 0) healthySince = now;
            if (now - healthySince >= 2_000_000_000L) {
                level = Math.min(MAX_LEVEL, level + 1);
                healthySince = now;
            }
        } else healthySince = -1;
    }

    public int level(long now) { return now < resumeAfter ? -1 : level; }
    public long nanos(long now) { return level(now) < 0 ? 0 : (2L + level * 2L) * 1_000_000L; }
    public int attempts(long now) { return level(now) < 0 ? 0 : 1 + level; }
}
