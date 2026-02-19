package myau.ui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.Myau;
import myau.module.Module;
import myau.module.modules.*;
import myau.module.modules.Timer;
import myau.ui.components.CategoryComponent;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ClickGui extends GuiScreen {

    public static List<Module> combatModules;
    public static List<Module> movementModules;
    public static List<Module> playerModules;
    public static List<Module> renderModules;
    public static List<Module> miscModules;

    private static ClickGui instance;
    private final File configFile = new File("./config/Myau/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;

    
    // Rise‑style background fade
    private float fade = 0f;

    public ClickGui() {
        instance = this;

        // === Your module registration stays exactly the same ===
        combatModules = new ArrayList<>();
        combatModules.add(Myau.moduleManager.getModule(AimAssist.class));
        combatModules.add(Myau.moduleManager.getModule(AutoClicker.class));
        combatModules.add(Myau.moduleManager.getModule(KillAura.class));
        combatModules.add(Myau.moduleManager.getModule(Wtap.class));
        combatModules.add(Myau.moduleManager.getModule(Velocity.class));
        combatModules.add(Myau.moduleManager.getModule(ServerLag.class));
        combatModules.add(Myau.moduleManager.getModule(Reach.class));
        combatModules.add(Myau.moduleManager.getModule(TargetStrafe.class));
        combatModules.add(Myau.moduleManager.getModule(NoHitDelay.class));
        combatModules.add(Myau.moduleManager.getModule(AntiFireball.class));
        combatModules.add(Myau.moduleManager.getModule(LagRange.class));
        combatModules.add(Myau.moduleManager.getModule(HitBox.class));
        combatModules.add(Myau.moduleManager.getModule(MoreKB.class));
        combatModules.add(Myau.moduleManager.getModule(Refill.class));
        combatModules.add(Myau.moduleManager.getModule(HitSelect.class));
        combatModules.add(Myau.moduleManager.getModule(BackTrack.class));
        combatModules.add(Myau.moduleManager.getModule(ClickAssits.class));
        combatModules.add(Myau.moduleManager.getModule(Criticals.class));
        combatModules.add(Myau.moduleManager.getModule(BlockHit.class));
        combatModules.add(Myau.moduleManager.getModule(Autoblock.class));

        movementModules = new ArrayList<>();
        movementModules.add(Myau.moduleManager.getModule(AntiAFK.class));
        movementModules.add(Myau.moduleManager.getModule(Fly.class));
        movementModules.add(Myau.moduleManager.getModule(FastBow.class));
        movementModules.add(Myau.moduleManager.getModule(Timer.class));
        movementModules.add(Myau.moduleManager.getModule(Speed.class));
        movementModules.add(Myau.moduleManager.getModule(LongJump.class));
        movementModules.add(Myau.moduleManager.getModule(Sprint.class));
        movementModules.add(Myau.moduleManager.getModule(SafeWalk.class));
        movementModules.add(Myau.moduleManager.getModule(Jesus.class));
        movementModules.add(Myau.moduleManager.getModule(Blink.class));
        movementModules.add(Myau.moduleManager.getModule(NoFall.class));
        movementModules.add(Myau.moduleManager.getModule(NoSlow.class));
        movementModules.add(Myau.moduleManager.getModule(KeepSprint.class));
        movementModules.add(Myau.moduleManager.getModule(Eagle.class));
        movementModules.add(Myau.moduleManager.getModule(NoJumpDelay.class));
        movementModules.add(Myau.moduleManager.getModule(AntiVoid.class));

        renderModules = new ArrayList<>();
        renderModules.add(Myau.moduleManager.getModule(ESP.class));
        renderModules.add(Myau.moduleManager.getModule(Chams.class));
        renderModules.add(Myau.moduleManager.getModule(FullBright.class));
        renderModules.add(Myau.moduleManager.getModule(Tracers.class));
        renderModules.add(Myau.moduleManager.getModule(NameTags.class));
        renderModules.add(Myau.moduleManager.getModule(Xray.class));
        renderModules.add(Myau.moduleManager.getModule(TargetHUD.class));
        renderModules.add(Myau.moduleManager.getModule(Indicators.class));
        renderModules.add(Myau.moduleManager.getModule(BedESP.class));
        renderModules.add(Myau.moduleManager.getModule(ItemESP.class));
        renderModules.add(Myau.moduleManager.getModule(ViewClip.class));
        renderModules.add(Myau.moduleManager.getModule(NoHurtCam.class));
        renderModules.add(Myau.moduleManager.getModule(HUD.class));
        renderModules.add(Myau.moduleManager.getModule(GuiModule.class));
        renderModules.add(Myau.moduleManager.getModule(ChestESP.class));
        renderModules.add(Myau.moduleManager.getModule(Trajectories.class));
        renderModules.add(Myau.moduleManager.getModule(Radar.class));
        renderModules.add(Myau.moduleManager.getModule(FPScounter.class));
        renderModules.add(Myau.moduleManager.getModule(Freelook.class));

        playerModules = new ArrayList<>();
        playerModules.add(Myau.moduleManager.getModule(AutoHeal.class));
        playerModules.add(Myau.moduleManager.getModule(FakeLag.class));
        playerModules.add(Myau.moduleManager.getModule(AutoTool.class));
        playerModules.add(Myau.moduleManager.getModule(ChestStealer.class));
        playerModules.add(Myau.moduleManager.getModule(InvManager.class));
        playerModules.add(Myau.moduleManager.getModule(InvWalk.class));
        playerModules.add(Myau.moduleManager.getModule(Scaffold.class));
        playerModules.add(Myau.moduleManager.getModule(AutoBlockIn.class));
        playerModules.add(Myau.moduleManager.getModule(AutoSwap.class));
        playerModules.add(Myau.moduleManager.getModule(SpeedMine.class));
        playerModules.add(Myau.moduleManager.getModule(FastPlace.class));
        playerModules.add(Myau.moduleManager.getModule(GhostHand.class));
        playerModules.add(Myau.moduleManager.getModule(MCF.class));
        playerModules.add(Myau.moduleManager.getModule(AntiDebuff.class));
        playerModules.add(Myau.moduleManager.getModule(FlagDetector.class));

        miscModules = new ArrayList<>();
        miscModules.add(Myau.moduleManager.getModule(Spammer.class));
        miscModules.add(Myau.moduleManager.getModule(BedNuker.class));
        miscModules.add(Myau.moduleManager.getModule(BedTracker.class));
        miscModules.add(Myau.moduleManager.getModule(LightningTracker.class));
        miscModules.add(Myau.moduleManager.getModule(NoRotate.class));
        miscModules.add(Myau.moduleManager.getModule(NickHider.class));
        miscModules.add(Myau.moduleManager.getModule(AntiObbyTrap.class));
        miscModules.add(Myau.moduleManager.getModule(AntiObfuscate.class));
        miscModules.add(Myau.moduleManager.getModule(AutoAnduril.class));
        miscModules.add(Myau.moduleManager.getModule(InventoryClicker.class));
        miscModules.add(Myau.moduleManager.getModule(Disabler.class));
        miscModules.add(Myau.moduleManager.getModule(ClientSpoofer.class));
        miscModules.add(Myau.moduleManager.getModule(AutoHypixel.class));

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combatModules);
        registered.addAll(movementModules);
        registered.addAll(renderModules);
        registered.addAll(playerModules);
        registered.addAll(miscModules);

        for (Module module : Myau.moduleManager.modules.values()) {
            if (!registered.contains(module)) {
                throw new RuntimeException(module.getClass().getName() + " is unregistered to click gui.");
            }
        }

        this.categoryList = new ArrayList<>();
        int topOffset = 20;

        CategoryComponent combat = new CategoryComponent("Combat", combatModules);
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 25;

        CategoryComponent movement = new CategoryComponent("Movement", movementModules);
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 25;

        CategoryComponent render = new CategoryComponent("Render", renderModules);
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 25;

        CategoryComponent player = new CategoryComponent("Player", playerModules);
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 25;

        CategoryComponent misc = new CategoryComponent("Misc", miscModules);
        misc.setY(topOffset);
        categoryList.add(misc);

        loadPositions();
    }

    public static ClickGui getInstance() {
        return instance;
    }

    @Override
    public void initGui() {
        super.initGui();
    }

    @Override
    public void drawScreen(int x, int y, float p) {

        // === Rise‑style fade background ===
        fade += (1f - fade) * 0.1f;
        drawRect(0, 0, width, height, new Color(0, 0, 0, (int)(fade * 120)).getRGB());

        // === Optional blur (Rise‑style) ===
        // If you want blur, I can generate a shader for you.

        for (CategoryComponent category : categoryList) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categoryList) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    @Override
    public void mouseClicked(int x, int y, int mouseButton) {
        for (CategoryComponent category : categoryList) {

            if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y) && mouseButton == 0) {
                category.mousePressed(true);
                category.xx = x - category.getX();
                category.yy = y - category.getY();
            }

            if (category.mousePressed(x, y) && mouseButton == 0) {
                category.setOpened(!category.isOpened());
            }

            if (category.isHovered(x, y) && mouseButton == 0) {
                category.setPin(!category.isPin());
            }

            if (category.isOpened()) {
                for (Component c : category.getModules()) {
                    c.mouseDown(x, y, mouseButton);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int x, int y, int mouseButton) {
        for (CategoryComponent category : categoryList) {
            if (mouseButton == 0) {
                category.mousePressed(false);
            }
        }

        for (CategoryComponent category : categoryList) {
            if (category.isOpened()) {
                for (Component component : category.getModules()) {
                    component.mouseReleased(x, y, mouseButton);
                }
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
            return;
        }

        for (CategoryComponent cat : categoryList) {
            if (cat.isOpened()) {
                for (Component component : cat.getModules()) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        savePositions();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : categoryList) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : categoryList) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
