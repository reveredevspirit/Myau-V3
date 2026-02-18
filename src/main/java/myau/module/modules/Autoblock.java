package myau.module.modules;

import myau.event.EventTarget;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AutoBlock - Automatically blocks (right-click hold) when an enemy is close and swinging.
 * Classic 1.8.9 style with range, timing, and lag compensation.
 */
public class Autoblock extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── Properties ───────────────────────────────────────────────────────────────
    public final PercentProperty range = new PercentProperty("Range", 4.5f); // 0–10 blocks, scaled

    public final IntProperty maxHurtTime = new IntProperty("Max Hurt Time", 8, 0, 10);

    public final IntProperty maxHoldDuration = new IntProperty("Max Hold Ticks", 5, 1, 20);

    public final IntProperty maxLagDuration = new IntProperty("Lag Comp Ticks", 3, 0, 10);

    public final BooleanProperty onlySword = new BooleanProperty("Only Sword", true);

    public final BooleanProperty onlyWhenSwinging = new BooleanProperty("Only Swinging", true);

    // ── Internal state ───────────────────────────────────────────────────────────
    private int blockTicks = 0;
    private boolean isBlocking = false;

    public Autoblock() {
        super("Autoblock", false);
        setKeybind(Keyboard.KEY_NONE); // optional manual toggle key
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != TickEvent.Type.PRE) return;
        if (mc.thePlayer == null || !mc.thePlayer.onGround) return;

        // Reset if disabled or no sword
        if (!isEnabled() || (onlySword.getValue() && !isHoldingSword())) {
            stopBlocking();
            return;
        }

        // Find closest valid target
        EntityLivingBase target = getClosestEnemy();

        if (target == null) {
            stopBlocking();
            return;
        }

        double distance = mc.thePlayer.getDistanceToEntity(target);

        // Range check (scaled percent → blocks)
        float realRange = range.getValue().floatValue() / 100f * 10f; // 0–10 blocks
        if (distance > realRange) {
            stopBlocking();
            return;
        }

        // Optional: only block if enemy is swinging arm
        if (onlyWhenSwinging.getValue() && target.swingProgressInt <= 0) {
            stopBlocking();
            return;
        }

        // Hurt time check (don't block if enemy just hit us)
        if (mc.thePlayer.hurtTime > maxHurtTime.getValue()) {
            stopBlocking();
            return;
        }

        // Start / continue blocking
        if (blockTicks < maxHoldDuration.getValue()) {
            startBlocking();
            blockTicks++;
        }

        // Lag compensation: keep blocking a few extra ticks after target leaves range
        if (blockTicks > 0 && distance > realRange + 0.5) {
            blockTicks--;
            if (blockTicks <= maxLagDuration.getValue()) {
                startBlocking();
            }
        }
    }

    private boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof net.minecraft.item.ItemSword;
    }

    private EntityLivingBase getClosestEnemy() {
        List<Entity> entities = mc.theWorld.loadedEntityList;

        return entities.stream()
                .filter(e -> e instanceof EntityLivingBase
                        && e != mc.thePlayer
                        && !(e instanceof EntityPlayer && TeamUtil.isFriend((EntityPlayer) e))
                        && mc.thePlayer.canEntityBeSeen(e))
                .map(e -> (EntityLivingBase) e)
                .min(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)))
                .orElse(null);
    }

    private void startBlocking() {
        if (!isBlocking) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem());
            isBlocking = true;
        }
    }

    private void stopBlocking() {
        if (isBlocking) {
            mc.thePlayer.stopUsingItem();
            isBlocking = false;
            blockTicks = 0;
        }
    }

    @Override
    public void onDisable() {
        stopBlocking();
        blockTicks = 0;
    }

    // Optional HUD suffix
    @Override
    public String[] getSuffix() {
        return isBlocking ? new String[]{"BLOCKING"} : new String[0];
    }
}
