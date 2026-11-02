package newearth.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import newearth.CatalogueScroll;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Adds catalogue scrolling to the existing GC screen inherited by EP. Client-only entry points. */
public final class CatalogueHooks {
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static final Field PARENT = field("selectedParent"), BODY = field("selectedBody"),
            ANIMATION = field("animateGrandchildren"), BORDER = field("BORDER_SIZE"), EDGE = field("BORDER_EDGE_SIZE");
    private static final Method ZOOMED = method("isZoomed"), CHILDREN = method("getChildren", Object.class);
    private CatalogueHooks() { }

    private static final class State {
        final CatalogueScroll scroll = new CatalogueScroll();
        Object root;
        int x, y, left, top, bottom, dragAnchor;
        boolean dragging;
    }

    private static State state(Object gui) { return STATES.computeIfAbsent(gui, key -> new State()); }

    public static void frame(Object object, int x, int y) {
        GuiScreen gui = (GuiScreen) object;
        State s = state(object); s.x = x; s.y = y;
        try {
            boolean zoomed = (boolean) ZOOMED.invoke(object);
            Object parent = PARENT.get(object), selected = BODY.get(object);
            Object root = zoomed && !(parent instanceof Planet) ? selected : parent;
            if (root != s.root) { s.root = root; s.scroll.offset = 0; s.dragging = false; }
            s.left = BORDER.getInt(null) + EDGE.getInt(null);
            s.top = s.left + 50; s.bottom = Math.max(s.top + 1, gui.height - s.left - 3);
            List<?> children = (List<?>) CHILDREN.invoke(object, root);
            s.scroll.contentHeight = children.size() * 14;
            if (!zoomed && children.contains(selected)) s.scroll.contentHeight += ANIMATION.getInt(object);
            s.scroll.viewportHeight = s.bottom - s.top; s.scroll.clamp();
            if (!Mouse.isButtonDown(0)) s.dragging = false;
            if (s.dragging) s.scroll.drag(y - s.top - s.dragAnchor);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported GC catalogue API", e); }
    }

    private static boolean inside(State s, int x, int y) {
        return x >= s.left && x <= s.left + 112 && y >= s.top && y < s.bottom;
    }

    public static int wheel(Object gui) {
        int delta = Mouse.getDWheel(); State s = state(gui);
        if (inside(s, s.x, s.y)) { s.scroll.wheel(delta); return 0; }
        return delta;
    }

    public static boolean click(Object gui, int x, int y, int button) {
        State s = state(gui);
        if (button != 0 || !inside(s, x, y) || x < s.left + 107 || s.scroll.maximum() == 0) return false;
        int thumb = s.top + s.scroll.thumbTop();
        s.dragAnchor = y >= thumb && y < thumb + s.scroll.thumbHeight() ? y - thumb : s.scroll.thumbHeight() / 2;
        s.dragging = true; s.scroll.drag(y - s.top - s.dragAnchor); return true;
    }

    public static boolean dragging(Object gui) { return state(gui).dragging; }

    public static int rowY(Object gui, int original, int x, int y) {
        State s = state(gui);
        return inside(s, x, y) ? s.scroll.rowY(original) : Integer.MIN_VALUE;
    }

    public static int begin(Object object, int prior, boolean root) {
        State s = state(object);
        if (root) {
            GuiScreen gui = (GuiScreen) object; Minecraft mc = Minecraft.getMinecraft();
            double sx = (double) mc.displayWidth / gui.width, sy = (double) mc.displayHeight / gui.height;
            GL11.glPushAttrib(GL11.GL_SCISSOR_BIT); GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) (s.left * sx), (int) ((gui.height - s.bottom) * sy),
                    (int) Math.ceil(112 * sx), (int) Math.ceil((s.bottom - s.top) * sy));
        }
        return s.scroll.rowY(prior);
    }

    public static void end(Object object, boolean root) {
        if (!root) return;
        GL11.glPopAttrib(); State s = state(object);
        if (s.scroll.maximum() == 0) return;
        int x = s.left + 108, y = s.top + s.scroll.thumbTop();
        Gui.drawRect(x, s.top, x + 3, s.bottom, 0xAA123344);
        Gui.drawRect(x, y, x + 3, y + s.scroll.thumbHeight(), s.dragging ? 0xFFFFFF88 : 0xFF55CCFF);
    }

    private static Field field(String name) {
        try { Field field = GuiCelestialSelection.class.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
    private static Method method(String name, Class<?>... args) {
        try { Method method = GuiCelestialSelection.class.getDeclaredMethod(name, args); method.setAccessible(true); return method; }
        catch (ReflectiveOperationException e) { throw new ExceptionInInitializerError(e); }
    }
}
