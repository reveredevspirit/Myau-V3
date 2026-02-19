package myau.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class ScissorUtil {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void enable(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale   = sr.getScaleFactor();
        int screenH = mc.displayHeight;

        int clampedX = Math.max(0, x);
        int clampedY = Math.max(0, y);
        int clampedW = Math.max(0, width);
        int clampedH = Math.max(0, height);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                clampedX * scale,
                screenH - (clampedY + clampedH) * scale,
                clampedW * scale,
                clampedH * scale
        );
    }

// Change from:
drawSolidRect(x + 163, y, x + 166, y + visibleHeight, 0xFF333333);
drawSolidRect(x + 163, barY, x + 166, barY + barH, 0xFF55AAFF);
// To:
drawSolidRect(x + 203, y, x + 206, y + visibleHeight, 0xFF333333);
drawSolidRect(x + 203, barY, x + 206, barY + barH, 0xFF55AAFF);
    
    public static void disable() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
