package myau.module.modules;

import myau.module.Module;
import myau.ui.clickgui.Rise6ClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private Rise6ClickGui clickGui;

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        if (clickGui == null) {
            clickGui = new Rise6ClickGui();
        }
        mc.displayGuiScreen(clickGui);
    }
}
