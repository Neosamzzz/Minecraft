package galaxyspace.core.telemetry;

import java.util.function.ToIntFunction;

/** Bound by rendered pixel width, including CJK and supplementary characters. */
public final class TelemetryTextLayout {
    private TelemetryTextLayout() { }
    public static String fit(String text, int maxWidth, ToIntFunction<String> width) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (width.applyAsInt(text) <= maxWidth) return text;
        String suffix = "...";
        if (width.applyAsInt(suffix) > maxWidth) return "";
        int low = 0, high = text.codePointCount(0, text.length());
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = text.substring(0, text.offsetByCodePoints(0, middle)) + suffix;
            if (width.applyAsInt(candidate) <= maxWidth) low = middle;
            else high = middle - 1;
        }
        return text.substring(0, text.offsetByCodePoints(0, low)) + suffix;
    }
}
