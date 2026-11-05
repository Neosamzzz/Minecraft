package newearth.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.Satellite;
import micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import newearth.CatalogueScroll;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/** Adds catalogue scrolling to the existing GC screen inherited by EP. Client-only entry points. */
public final class CatalogueHooks {
    private static final Map<Object, State> STATES = new WeakHashMap<>();
    private static final Field PARENT = field("selectedParent"), BODY = field("selectedBody"),
            ANIMATION = field("animateGrandchildren"), BORDER = field("BORDER_SIZE"), EDGE = field("BORDER_EDGE_SIZE"), VIEW = field("viewState");
    private static final Method ZOOMED = method("isZoomed"), CHILDREN = method("getChildren", Object.class);
    private static final int WHEEL_UNITS = wheelUnits();
    private static int wheelUnits() {
        try {
            Class.forName("org.lwjglx.input.Mouse", false, CatalogueHooks.class.getClassLoader());
            return 1;
        } catch (ClassNotFoundException ignored) { return 120; }
    }
    private CatalogueHooks() { }

    private static final class State {
        final CatalogueScroll scroll = new CatalogueScroll();
        final CatalogueScroll details = new CatalogueScroll();
        Object root;
        Object detailsBody;
        int x, y, left, top, bottom, dragAnchor;
        int detailsLeft, detailsTop, detailsBottom, detailsAnchor;
        boolean dragging, detailsDragging, detailsEnabled;
        List<String> detailLines = new ArrayList<>();
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
            s.detailsEnabled = selected != null && !(selected instanceof Satellite) && !VIEW.get(object).toString().equals("PROFILE");
            if (selected != s.detailsBody) {
                s.detailsBody = selected; s.details.offset = 0; s.detailsDragging = false;
                s.detailLines = selected == null || selected instanceof Satellite ? new ArrayList<>() : PlanetFacts.describe((CelestialBody) selected);
            }
            s.detailsLeft = gui.width - s.left - 92; s.detailsTop = s.left + 30;
            int lines = 0;
            for (String line : s.detailLines) lines += Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(line, 82).size();
            s.details.contentHeight = lines * 11;
            s.details.viewportHeight = Math.max(1, Math.min(100, gui.height - s.left - 3 - s.detailsTop));
            s.detailsBottom = s.detailsTop + s.details.viewportHeight; s.details.clamp();
            if (!Mouse.isButtonDown(0)) { s.dragging = false; s.detailsDragging = false; }
            if (s.dragging) s.scroll.drag(y - s.top - s.dragAnchor);
            if (s.detailsDragging) s.details.drag(y - s.detailsTop - s.detailsAnchor);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported GC catalogue API", e); }
    }

    private static boolean insideDetails(State s, int x, int y) {
        return s.detailsEnabled && x >= s.detailsLeft && x < s.detailsLeft + 88 && y >= s.detailsTop && y < s.detailsBottom;
    }

    private static boolean inside(State s, int x, int y) {
        return x >= s.left && x <= s.left + 112 && y >= s.top && y < s.bottom;
    }

    public static int wheel(Object gui) {
        int delta = Mouse.getDWheel();
        if (delta == 0) return 0;
        State s = state(gui);
        if (insideDetails(s, s.x, s.y) && s.details.wheel(delta)) return 0;
        if (inside(s, s.x, s.y) && s.scroll.wheel(delta)) return 0;
        // Cleanroom reports notches, whereas LWJGL 2 reports about 120 units per notch.
        return CatalogueScroll.zoomWheel(delta, WHEEL_UNITS);
    }

    public static boolean click(Object gui, int x, int y, int button) {
        State s = state(gui);
        if (button == 0 && insideDetails(s, x, y) && x >= s.detailsLeft + 84 && s.details.maximum() > 0) {
            int thumb = s.detailsTop + s.details.thumbTop();
            s.detailsAnchor = y >= thumb && y < thumb + s.details.thumbHeight() ? y - thumb : s.details.thumbHeight() / 2;
            s.detailsDragging = true; s.details.drag(y - s.detailsTop - s.detailsAnchor); return true;
        }
        if (button != 0 || !inside(s, x, y) || x < s.left + 107 || s.scroll.maximum() == 0) return false;
        int thumb = s.top + s.scroll.thumbTop();
        s.dragAnchor = y >= thumb && y < thumb + s.scroll.thumbHeight() ? y - thumb : s.scroll.thumbHeight() / 2;
        s.dragging = true; s.scroll.drag(y - s.top - s.dragAnchor); return true;
    }

    public static boolean dragging(Object gui) { State s = state(gui); return s.dragging || s.detailsDragging; }

    public static boolean legacyDetails(Object selected) { return false; }

    public static void drawFacts(Object object) {
        State s = state(object);
        if (!s.detailsEnabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.fontRenderer.drawString(I18n.format("newearth.map.details"), s.detailsLeft + 2, s.left + 18, 0x55CCFF);
        clip(object, s.detailsLeft, s.detailsTop, 88, s.details.viewportHeight);
        int y = s.detailsTop - s.details.offset;
        for (String line : s.detailLines) {
            for (String part : mc.fontRenderer.listFormattedStringToWidth(line, 82)) {
                mc.fontRenderer.drawString(part, s.detailsLeft, y, 0xDDDDDD); y += 11;
            }
        }
        GL11.glPopAttrib();
        if (s.details.maximum() > 0) {
            int x = s.detailsLeft + 85, top = s.detailsTop + s.details.thumbTop();
            Gui.drawRect(x, s.detailsTop, x + 2, s.detailsBottom, 0xAA123344);
            Gui.drawRect(x, top, x + 2, top + s.details.thumbHeight(), 0xFF55CCFF);
        }
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void clip(Object object, int x, int y, int width, int height) {
        GuiScreen gui = (GuiScreen) object; Minecraft mc = Minecraft.getMinecraft();
        double sx = (double) mc.displayWidth / gui.width, sy = (double) mc.displayHeight / gui.height;
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT); GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * sx), (int) ((gui.height - y - height) * sy), (int) Math.ceil(width * sx), (int) Math.ceil(height * sy));
    }

    public static int rowY(Object gui, int original, int x, int y) {
        State s = state(gui);
        return inside(s, x, y) ? s.scroll.rowY(original) : Integer.MIN_VALUE;
    }

    public static int begin(Object object, int prior, boolean root) {
        State s = state(object);
        if (root) {
            clip(object, s.left, s.top, 112, s.bottom - s.top);
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
