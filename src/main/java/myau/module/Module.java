package myau.module;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private boolean enabled = false;  // or "toggled" if your base uses that name

    // Animation fields for ArrayList (slide + fade)
    private long lastToggleTime = 0L;
    private float animationProgress = 0.0f;  // 0.0 = hidden/slid out, 1.0 = fully visible

    public Module(String name) {
        this.name = name;
    }

    // Toggle method - adjust if your base has different logic (e.g. setEnabled, toggleModule)
    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;

        this.enabled = enabled;
        lastToggleTime = System.currentTimeMillis();

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Animation update - call this every tick/frame
    public void updateAnimation() {
        float target = isEnabled() ? 1.0f : 0.0f;
        animationProgress = MathHelper.lerp(0.14f, animationProgress, target);
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    // Optional reset (e.g. after loading config)
    public void resetAnimation() {
        animationProgress = isEnabled() ? 1.0f : 0.0f;
    }

    // Lifecycle - override in child modules (e.g. KillAura, Fly)
    protected void onEnable() {
        // Add enable logic here if needed
    }

    protected void onDisable() {
        // Add disable logic here if needed
    }

    // Tick method - many bases call this per module every tick
    public void onUpdate() {
        // Optional: put animation update here as fallback
        // updateAnimation();
    }

    // Render if module draws something itself (rare for arraylist)
    public void onRender2D() {
        // optional
    }

    // Basic getters
    public String getName() {
        return name;
    }

    // If your modules have display names/suffixes, add:
    // public String getDisplayName() { return name; }
}
