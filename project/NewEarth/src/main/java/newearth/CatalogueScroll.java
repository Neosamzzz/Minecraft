package newearth;

/** Pixel-based list geometry shared by drawing, wheel input, dragging and hit tests. */
public final class CatalogueScroll {
    public int offset;
    public int contentHeight;
    public int viewportHeight;

    public int maximum() { return Math.max(0, contentHeight - viewportHeight); }
    public int rowY(int original) { return original - offset; }
    public void clamp() { offset = Math.max(0, Math.min(maximum(), offset)); }
    public boolean wheel(int delta) {
        int before = offset;
        offset -= Integer.signum(delta) * 28;
        clamp();
        return offset != before;
    }
    public int thumbHeight() {
        return Math.min(viewportHeight, Math.max(12, viewportHeight * viewportHeight / Math.max(1, contentHeight)));
    }
    public int thumbTop() {
        return maximum() == 0 ? 0 : (int) ((long) offset * (viewportHeight - thumbHeight()) / maximum());
    }
    public void drag(int thumbTop) {
        int travel = viewportHeight - thumbHeight();
        offset = travel <= 0 ? 0 : (int) Math.round((double) thumbTop * maximum() / travel);
        clamp();
    }
}
