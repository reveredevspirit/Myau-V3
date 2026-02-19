package myau.ui.clickgui;

import myau.module.Module;
import myau.Myau;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Rise6ClickGui extends GuiScreen {

    private final List<SidebarCategory> categories = new ArrayList<>();
    private SidebarCategory selectedCategory;

    private SearchBar searchBar;
    private ModulePanel modulePanel;

    public Rise6ClickGui() {

        // === Create sidebar categories ===
        categories.add(new SidebarCategory("Combat"));
        categories.add(new SidebarCategory("Movement"));
        categories.add(new SidebarCategory("Player"));
        categories.add(new SidebarCategory("Render"));
        categories.add(new SidebarCategory("Misc"));

        // Default selected category
        selectedCategory = categories.get(0);

        // Create search bar + module panel
        searchBar = new SearchBar();
        modulePanel = new ModulePanel(selectedCategory);
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        ScaledResolution sr = new ScaledResolution(mc);

        // === Background (Rise 6 uses a dark fade) ===
        drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0xAA000000);

        // === Draw sidebar ===
        int yOffset = 40;
        for (SidebarCategory cat : categories) {
            cat.render(10, yOffset, mouseX, mouseY, selectedCategory == cat);
            yOffset += 28;
        }

        // === Draw search bar ===
        searchBar.render(130, 30, mouseX, mouseY);

        // === Draw module panel ===
        modulePanel.render(130, 60, mouseX, mouseY, searchBar.getText());

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {

        // Sidebar click detection
        for (SidebarCategory cat : categories) {
            if (cat.mouseClicked(mouseX, mouseY, button)) {
                selectedCategory = cat;
                modulePanel.setCategory(cat);
                return;
            }
        }

        searchBar.mouseClicked(mouseX, mouseY, button);
        modulePanel.mouseClicked(mouseX, mouseY, button);

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        modulePanel.mouseReleased(mouseX, mouseY, button);
        super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {

        // ESC closes GUI
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }

        // Type into search bar
        if (searchBar.keyTyped(typedChar, keyCode)) {
            return;
        }

        // Pass keys to module panel (for text boxes, etc.)
        modulePanel.keyTyped(typedChar, keyCode);

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
