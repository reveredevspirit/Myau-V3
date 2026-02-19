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
    public final BooleanProperty invertPitch = new BooleanProperty("Invert Pitch", false);
    public final BooleanProperty smoothReturn = new BooleanProperty("Smooth Return", true);
    public final PercentProperty returnSpeed = new PercentProperty("Return Speed", 60);

    private boolean isActive = false;
    private boolean wasPressed = false;
    private boolean wasActiveLastTick = false;

    private float storedYaw;
    private float storedPitch;
    private float cameraYaw;
    private float cameraPitch;

    public Freelook() {
        super("Freelook", true);
        this.setKey(Keyboard.KEY_6);  // default keybind: 6
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;

        boolean keyDown = Keyboard.isKeyDown(this.getKey());

        // Activation logic
        if (mode.getValue() == 0) { // Hold
            isActive = keyDown && mc.currentScreen == null;
        } else { // Toggle
            if (keyDown && !wasPressed) {
                isActive = !isActive;
                wasPressed = true;
            }
            if (!keyDown) wasPressed = false;
        }

        if (isActive && mc.currentScreen == null) {

            // On first activation tick: capture real player angles
            if (!wasActiveLastTick) {
                storedYaw   = mc.thePlayer.rotationYaw;
                storedPitch = mc.thePlayer.rotationPitch;
                cameraYaw   = storedYaw;
                cameraPitch = storedPitch;

                // Clear any pending mouse movement to prevent jump
                while (Mouse.next()) {}  // drain queue
                Mouse.getDX();
                Mouse.getDY();
            }

            // Read mouse deltas
            int dx = Mouse.getDX();
            int dy = Mouse.getDY();

            float sensMult = sensitivity.getValue().floatValue() / 100f * 0.15f;

            cameraYaw += (float) dx * sensMult;

            float pitchChange = (float) dy * sensMult;
            if (invertPitch.getValue()) pitchChange = -pitchChange;
            cameraPitch -= pitchChange;

            cameraPitch = Math.max(-90f, Math.min(90f, cameraPitch));

            // Freeze body / movement direction completely
            mc.thePlayer.rotationYaw        = storedYaw;
            mc.thePlayer.rotationPitch      = storedPitch;
            mc.thePlayer.prevRotationYaw    = storedYaw;
            mc.thePlayer.prevRotationPitch  = storedPitch;

            // Prevent renderYawOffset from drifting (affects body/legs in some views)
            mc.thePlayer.renderYawOffset    = storedYaw;
            mc.thePlayer.prevRenderYawOffset = storedYaw;
        }

        // Deactivation: smooth return of camera/head only
        if (!isActive && wasActiveLastTick) {
            // Optional: force one last freeze in case of edge case
            if (mc.thePlayer != null) {
                mc.thePlayer.rotationYaw = storedYaw;
                mc.thePlayer.rotationPitch = storedPitch;
            }
        }

        wasActiveLastTick = isActive;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled()) return;

        if (isActive || (smoothReturn.getValue() && Math.abs(cameraYaw - storedYaw) > 0.25f)) {
            // Only rotate the view/head — body stays locked
            mc.thePlayer.rotationYawHead = cameraYaw;

            // Optional: if arms/hands look wrong in some animation mods, try:
            // mc.thePlayer.renderYawOffset = cameraYaw;  // but usually keep frozen

            // During smooth return, interpolate camera back
            if (!isActive && smoothReturn.getValue()) {
                float speed = returnSpeed.getValue().floatValue() / 100f * 0.45f;
                cameraYaw   += (storedYaw   - cameraYaw)   * speed;
                cameraPitch += (storedPitch - cameraPitch) * speed;

                // Snap when close enough
                if (Math.abs(cameraYaw - storedYaw) < 0.6f && Math.abs(cameraPitch - storedPitch) < 0.6f) {
                    cameraYaw   = storedYaw;
                    cameraPitch = storedPitch;
                    mc.thePlayer.rotationYawHead = storedYaw;
                }
            }
        } else {
            // Ensure reset when done
            if (mc.thePlayer != null) {
                mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw;
            }
        }
    }

    @Override
    public void onDisabled() {
        isActive = false;
        wasPressed = false;
        wasActiveLastTick = false;

        if (mc.thePlayer != null) {
            mc.thePlayer.rotationYawHead     = mc.thePlayer.rotationYaw;
            mc.thePlayer.renderYawOffset     = mc.thePlayer.rotationYaw;
            mc.thePlayer.prevRenderYawOffset = mc.thePlayer.rotationYaw;
        }
    }

    @Override
    public String[] getSuffix() {
        if (!isActive) return new String[0];
        return new String[]{ mode.getValue() == 0 ? "HOLD" : "ON" };
    }
}
