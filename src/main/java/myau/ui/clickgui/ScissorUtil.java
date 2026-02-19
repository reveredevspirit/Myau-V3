package myau.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class ScissorUtil {

    public static void enable(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        int scale = sr.getScaleFactor();
        int screenH = Minecraft.getMinecraft().displayHeight;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                x * scale,
                screenH - (y + height) * scale,
                width * scale,
                height * scale
        );
    }

    public static void disable() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
