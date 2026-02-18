package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Freelook extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Hold", "Toggle"});
    public final PercentProperty sensitivity = new PercentProperty("Sensitivity", 100);
    public final BooleanProperty smoothReturn = new BooleanProperty("Smooth Return", true);
    public final PercentProperty returnSpeed = new PercentProperty("Return Speed", 50);

    private boolean isActive = false;
    private boolean wasPressed = false;

    private float currentYawOffset   = 0.0f;
    private float currentPitchOffset = 0.0f;
    private float targetYawOffset    = 0.0f;
    private float targetPitchOffset  = 0.0f;

    public Freelook() {
        super("Freelook", true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;

        // ── Activation ───────────────────────────────────────────────────────
        if (mode.getValue() == 0) {
            // Hold mode
            isActive = Keyboard.isKeyDown(Keyboard.KEY_F);
        } else {
            // Toggle mode
            if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
                if (!wasPressed) {
                    isActive   = !isActive;
                    wasPressed = true;
                }
            } else {
                wasPressed = false;
            }
        }

        // ── Smooth return when inactive ──────────────────────────────────────
        if (!isActive && smoothReturn.getValue()) {
            targetYawOffset   = 0.0f;
            targetPitchOffset = 0.0f;
            float speed = returnSpeed.getValue().floatValue() / 100f * 0.25f;
            currentYawOffset   += (targetYawOffset   - currentYawOffset)   * speed;
            currentPitchOffset += (targetPitchOffset - currentPitchOffset) * speed;
            if (Math.abs(currentYawOffset)   < 0.05f) currentYawOffset   = 0.0f;
            if (Math.abs(currentPitchOffset) < 0.05f) currentPitchOffset = 0.0f;
        }

        // ── Poll mouse when active ───────────────────────────────────────────
        if (isActive && mc.currentScreen == null) {
            int dx = Mouse.getDX();
            int dy = Mouse.getDY();
            if (dx != 0 || dy != 0) {
                float sensMult  = sensitivity.getValue().floatValue() / 100f;
                float yawDelta  =  dx * 0.15f * sensMult;
                float pitchDelta = dy * 0.15f * sensMult * -1f;
                targetYawOffset   += yawDelta;
                targetPitchOffset += pitchDelta;
                targetPitchOffset  = Math.max(-90.0f, Math.min(90.0f, targetPitchOffset));
            }
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled()) return;
        if (!isActive && currentYawOffset == 0.0f && currentPitchOffset == 0.0f) return;

        mc.thePlayer.renderYawOffset    = mc.thePlayer.rotationYaw   + currentYawOffset;
        mc.thePlayer.rotationYawHead    = mc.thePlayer.rotationYaw   + currentYawOffset;
        mc.thePlayer.rotationPitchHead  = mc.thePlayer.rotationPitch + currentPitchOffset;

        float lerpFactor   = 0.85f;
        currentYawOffset   = lerp(currentYawOffset,   targetYawOffset,   lerpFactor);
        currentPitchOffset = lerp(currentPitchOffset, targetPitchOffset, lerpFactor);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    @Override
    public void onDisable() {
        // Reset camera state so the view snaps back instantly on disable
        isActive          = false;
        wasPressed        = false;
        currentYawOffset   = 0.0f;
        currentPitchOffset = 0.0f;
        targetYawOffset    = 0.0f;
        targetPitchOffset  = 0.0f;
    }

    @Override
    public String[] getSuffix() {
        if (!isActive) return new String[0];
        return new String[]{ mode.getValue() == 0 ? "HOLD" : "ON" };
    }
}
