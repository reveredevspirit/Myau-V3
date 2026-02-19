package myau.ui.hud;

import myau.Myau;
import myau.module.Module;
import myau.module.modules.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ArraylistHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public void render() {
        ScaledResolution sr = new ScaledResolution(mc);
        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        if (hud == null || !hud.isEnabled()) return;

        Color color = hud.getColor(System.currentTimeMillis());

        // Build sorted list of enabled non-hidden modules
        List<Module> enabled = new ArrayList<>();
        for (Module m : Myau.moduleManager.modules.values()) {
            if (m.isEnabled() && !m.isHidden()) enabled.add(m);
        }

        // Sort by total display width descending
        enabled.sort(Comparator.comparingInt((Module m) -> {
            String[] suffix = m.getSuffix();
            String suffixStr = suffix.length > 0 ? " " + suffix[0] : "";
            return mc.fontRendererObj.getStringWidth(
                    m.getName().toLowerCase() + suffixStr.toLowerCase());
        }).reversed());

        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        int lineHeight = fontHeight + 1; // very tight spacing
        int paddingX   = 5;
        int paddingY   = 2;

        for (int i = 0; i < enabled.size(); i++) {
            Module module = enabled.get(i);

            String name   = module.getName().toLowerCase();
            String[] suffixArr = module.getSuffix();
            String suffix = suffixArr.length > 0 ? " " + suffixArr[0].toLowerCase() : "";

            int nameW   = mc.fontRendererObj.getStringWidth(name);
            int suffixW = mc.fontRendererObj.getStringWidth(suffix);
            int totalW  = nameW + suffixW;

            int bgW = totalW + paddingX * 2;
            int bgH = fontHeight + paddingY * 2;

            // Right-aligned position
            int bgX = sr.getScaledWidth() - bgW - 2;
            int bgY = 4 + i * (lineHeight + paddingY * 2);

            // Pill background
            drawPill(bgX, bgY, bgW, bgH, 0x99000000);

            int textX = bgX + paddingX;
            int textY = bgY + paddingY;

            // Module name in white
            mc.fontRendererObj.drawString(name, textX, textY, 0xFFFFFFFF);

            // Suffix in HUD color
            if (!suffix.isEmpty()) {
                mc.fontRendererObj.drawString(suffix, textX + nameW, textY, color.getRGB() | 0xFF000000);
            }
        }
    }

    // Draws a rounded pill background
    private void drawPill(int x, int y, int width, int height, int color) {
        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8  & 0xFF) / 255f;
        float b = (color       & 0xFF) / 255f;

        float radius = height / 2f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);

        // Center rect
        drawQuad(x + radius, y, x + width - radius, y + height);

        // Left cap
        drawCorner(x + radius, y + radius, radius, 90, 270);

        // Right cap
        drawCorner(x + width - radius, y + radius, radius, 270, 450);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void drawQuad(float x1, float y1, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x1, y2);
        GL11.glEnd();
    }

    private void drawCorner(float cx, float cy, float radius, int startAngle, int endAngle) {
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = startAngle; i <= endAngle; i += 5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f(
                    cx + (float)(Math.cos(angle) * radius),
                    cy + (float)(Math.sin(angle) * radius)
            );
        }
        GL11.glEnd();
    }
}
