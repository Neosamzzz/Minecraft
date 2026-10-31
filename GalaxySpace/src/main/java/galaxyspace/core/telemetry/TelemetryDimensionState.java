package galaxyspace.core.telemetry;

public interface TelemetryDimensionState {
    void galaxyspace$receiveDimension(boolean tracked, boolean known, int dimension, String key, String fallback);
    boolean galaxyspace$hasDimensionTarget();
    boolean galaxyspace$dimensionKnown();
    int galaxyspace$targetDimension();
    String galaxyspace$dimensionKey();
    String galaxyspace$dimensionFallback();
}
