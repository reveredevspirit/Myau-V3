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
        ScaledResolution sr = new ScaledResolution(mc);
        HUD hud = (HUD) Myau.moduleManager.getModule(HUD.class);
        if (hud == null || !hud.isEnabled()) return;

        Color color = hud.getColor(System.currentTimeMillis());
        boolean shadow = hud.shadow.getValue();

        // Build sorted list of enabled modules
        List<Module> enabled = new ArrayList<>();
        for (Module m : Myau.moduleManager.modules.values()) {
            if (m.isEnabled() && !m.isHidden()) enabled.add(m);
        }

        // Sort by total display width descending (name + suffix)
        enabled.sort(Comparator.comparingInt((Module m) -> {
            String[] suffix = m.getSuffix();
            String suffixStr = suffix.length > 0 ? " " + suffix[0] : "";
            return mc.fontRendererObj.getStringWidth(m.getName() + suffixStr);
        }).reversed());

        int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
        int lineHeight = fontHeight + 2;

        for (int i = 0; i < enabled.size(); i++) {
            Module module = enabled.get(i);

            String name = module.getName();
            String[] suffixArr = module.getSuffix();
            String suffix = suffixArr.length > 0 ? " " + suffixArr[0] : "";

            int nameW   = mc.fontRendererObj.getStringWidth(name);
            int suffixW = mc.fontRendererObj.getStringWidth(suffix);
            int totalW  = nameW + suffixW;

            // Right-aligned, flush to screen edge
            int x = sr.getScaledWidth() - totalW - 4;
            int y = 4 + i * lineHeight;

            // Draw name in white
            if (shadow) {
                mc.fontRendererObj.drawStringWithShadow(name, x, y, 0xFFFFFFFF);
            } else {
                mc.fontRendererObj.drawString(name, x, y, 0xFFFFFFFF);
            }

            // Draw suffix in HUD color
            if (!suffix.isEmpty()) {
                if (shadow) {
                    mc.fontRendererObj.drawStringWithShadow(suffix, x + nameW, y, color.getRGB());
                } else {
                    mc.fontRendererObj.drawString(suffix, x + nameW, y, color.getRGB());
                }
            }
        }
    }
}
