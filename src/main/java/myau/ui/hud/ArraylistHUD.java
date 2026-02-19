package myau.ui.hud;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArraylistHUD {

    private final Minecraft mc = Minecraft.getMinecraft();

    public void render() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        HUD hudModule = (HUD) Myau.moduleManager.getModule(HUD.class);
        if (hudModule == null || !hudModule.isEnabled()) return; // optional: only render if HUD module toggled

        // Collect & sort enabled modules once (efficient)
        List<Module> enabled = new ArrayList<>(Myau.moduleManager.modules.values());
        enabled.removeIf(m -> !m.isEnabled());
        enabled.sort(Comparator.comparingInt(m -> -mc.fontRendererObj.getStringWidth(m.getName()))); // longest first

        int y = 4; // start a bit higher to avoid overlap; make configurable later
        int spacing = mc.fontRendererObj.FONT_HEIGHT + 2;

        for (int i = 0; i < enabled.size(); i++) {
            Module module = enabled.get(i);
            String name = module.getName(); // ← add suffixes later if wanted (e.g. module.getSuffix())

            int textWidth = mc.fontRendererObj.getStringWidth(name);
            int x = sr.getScaledWidth() - textWidth - 4; // 4px padding from right

            Color color = hudModule.getColor(System.currentTimeMillis() + (i * 150L)); // offset per module for nicer rainbow

            // Optional semi-transparent background (very common in good clients)
            drawBackground(x - 3, y - 1, textWidth + 6, spacing - 1, new Color(0, 0, 0, 90));

            // Draw with shadow for readability
            mc.fontRendererObj.drawStringWithShadow(name, x, y, color.getRGB());

            y += spacing;
        }
    }

    // Helper – add this (or use your existing RenderUtil if you have one)
    private void drawBackground(int x, int y, int width, int height, int color) {
        // Simple filled rect – you can replace with RenderUtil.drawRect if exists
        // This uses vanilla-style GL calls (common in 1.8.9 clients)
        // But for simplicity, if you have no RenderUtil yet:
        net.minecraft.client.renderer.GlStateManager.enableBlend();
        net.minecraft.client.renderer.GlStateManager.disableTexture2D();
        net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        org.lwjgl.opengl.GL11.glGetFloat(org.lwjgl.opengl.GL11.GL_CURRENT_COLOR, buffer);

        org.lwjgl.opengl.GL11.glColor4f(
                (color >> 16 & 255) / 255f,
                (color >> 8 & 255) / 255f,
                (color & 255) / 255f,
                ((color >> 24 & 255) / 255f) * 0.6f // slight alpha adjust
        );

        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
        org.lwjgl.opengl.GL11.glVertex2f(x, y);
        org.lwjgl.opengl.GL11.glVertex2f(x + width, y);
        org.lwjgl.opengl.GL11.glVertex2f(x + width, y + height);
        org.lwjgl.opengl.GL11.glVertex2f(x, y + height);
        org.lwjgl.opengl.GL11.glEnd();

        net.minecraft.client.renderer.GlStateManager.enableTexture2D();
        net.minecraft.client.renderer.GlStateManager.disableBlend();
        org.lwjgl.opengl.GL11.glColor4f(buffer.get(0), buffer.get(1), buffer.get(2), buffer.get(3));
    }
}
