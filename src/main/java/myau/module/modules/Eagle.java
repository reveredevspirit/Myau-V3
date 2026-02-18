package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PlayerUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

/**
 * Eagle / Safewalk — myau fork
 *
 * Base logic:       original myau Eagle
 * Edge-offset logic: ported from Raven BS V2 Safewalk
 *
 * Edge offset works by simulating one tick of movement and checking how close
 * the projected position is to a block edge. If we are within `edgeOffset`
 * blocks of an edge (and the tile beneath that edge is air) we force a sneak
 * so the player stops before stepping off.
 */
public class Eagle extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── Original myau timing properties ──────────────────────────────────────
    public final IntProperty minDelay       = new IntProperty("min-delay",      2,    0, 10);
    public final IntProperty maxDelay       = new IntProperty("max-delay",      3,    0, 10);

    // ── Original myau filter properties ──────────────────────────────────────
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check",   true);
    public final BooleanProperty pitchCheck     = new BooleanProperty("pitch-check",       true);
    public final BooleanProperty blocksOnly     = new BooleanProperty("blocks-only",       true);
    public final BooleanProperty sneakOnly      = new BooleanProperty("sneaking-only",     false);

    // ── Raven BS V2 edge-offset properties ───────────────────────────────────
    /**
     * How many blocks from an edge the player must be before we auto-sneak.
     * 0.0 = disabled / vanilla behaviour; 0.5 is a safe general value.
     */
    public final FloatProperty edgeOffset    = new FloatProperty("edge-offset",    0.5f,  0.0f, 1.0f);

    /**
     * After releasing sneak we wait this many ticks before sneaking again.
     * Mirrors Raven's "unsneak delay" to avoid jitter.
     */
    public final IntProperty unsneakDelay    = new IntProperty("unsneak-delay",    3,    0, 10);

    /**
     * Only activate edge-offset logic while holding a block in hand.
     * Mirrors Raven's "holding blocks" sub-option.
     */
    public final BooleanProperty edgeBlocksOnly = new BooleanProperty("edge-blocks-only", true);

    // ── Internal state ────────────────────────────────────────────────────────
    private int sneakDelay    = 0;
    private int unsneakTicks  = 0;   // counts down after we release sneak

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public Eagle() {
        super("Eagle", false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core helpers (original myau)
    // ─────────────────────────────────────────────────────────────────────────

    private boolean canMoveSafely() {
        double[] offset = MoveUtil.predictMovement();
        return PlayerUtil.canMove(
                mc.thePlayer.motionX + offset[0],
                mc.thePlayer.motionZ + offset[1]
        );
    }

    private boolean shouldSneak() {
        if (directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        }
        if (pitchCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        }
        if (sneakOnly.getValue() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return false;
        }
        if (blocksOnly.getValue() && !ItemUtil.isHoldingBlock()) {
            return false;
        }
        return mc.thePlayer.onGround;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Edge-offset logic (ported from Raven BS V2)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns true when the player is about to walk off an edge within the
     * configured {@link #edgeOffset} distance.
     *
     * The algorithm simulates one physics tick of movement from the player's
     * current position (using current motion + input forward/strafe) and then
     * walks the four axis-aligned corners of the player's AABB at that future
     * position. If any corner hangs over air we trigger.
     */
    private boolean isNearEdge() {
        float offset = edgeOffset.getValue();
        if (offset <= 0.0f) return false;
        if (!mc.thePlayer.onGround) return false;
        if (edgeBlocksOnly.getValue() && !ItemUtil.isHoldingBlock()) return false;

        // Current position
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY;
        double pz = mc.thePlayer.posZ;

        // Simulate one tick of horizontal motion
        double mx = mc.thePlayer.motionX;
        double mz = mc.thePlayer.motionZ;

        // Apply a small amount of input influence (mirrors Raven's Simulation.tick)
        float forward = mc.thePlayer.movementInput.moveForward;
        float strafe  = mc.thePlayer.movementInput.moveStrafe;
        double yaw    = Math.toRadians(mc.thePlayer.rotationYaw);
        mx += (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * 0.02;
        mz += ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * 0.02;

        double simX = px + mx;
        double simZ = pz + mz;
        double floorY = Math.floor(py);

        // Player AABB half-width is 0.3; check corners with edge-offset expansion
        double w = 0.3 + offset;
        double[][] corners = {
            { simX + w, simZ + w },
            { simX + w, simZ - w },
            { simX - w, simZ + w },
            { simX - w, simZ - w }
        };

        for (double[] corner : corners) {
            int bx = (int) Math.floor(corner[0]);
            int by = (int) floorY - 1;   // block directly below player feet at that XZ
            int bz = (int) Math.floor(corner[1]);

            Block below = getBlockAt(bx, by, bz);
            if (below instanceof BlockAir || below == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safe wrapper around {@link Minecraft#theWorld} block lookup.
     */
        private Block getBlockAt(int x, int y, int z) {
    try {
        return mc.theWorld.getChunkFromBlockCoords(new BlockPos(x, y, z))
                .getBlock(new BlockPos(x, y, z));
    } catch (Exception e) {
        return null;
    }
}

    // ─────────────────────────────────────────────────────────────────────────
    //  Event handlers
    // ─────────────────────────────────────────────────────────────────────────

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (!isEnabled() || event.getType() != EventType.PRE) return;

        if (sneakDelay > 0)   sneakDelay--;
        if (unsneakTicks > 0) unsneakTicks--;

        if (sneakDelay == 0 && canMoveSafely()) {
            sneakDelay = RandomUtils.nextInt(minDelay.getValue(), maxDelay.getValue() + 1);
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled() || mc.currentScreen != null) return;

        // ── sneakOnly passthrough (original myau behaviour) ───────────────────
        if (sneakOnly.getValue()
                && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())
                && shouldSneak()) {
            mc.thePlayer.movementInput.sneak = false;
            mc.thePlayer.movementInput.moveForward /= 0.3F;
            mc.thePlayer.movementInput.moveStrafe  /= 0.3F;
        }

        // ── Standard sneak / edge-offset logic ───────────────────────────────
        if (!mc.thePlayer.movementInput.sneak && unsneakTicks == 0) {

            boolean wantSneak = false;

            // Original timing-based trigger
            if (shouldSneak() && (sneakDelay > 0 || canMoveSafely())) {
                wantSneak = true;
            }

            // Raven-style edge-offset trigger (independent of shouldSneak filters
            // so it works even when looking forward, etc.)
            if (isNearEdge()) {
                wantSneak = true;
            }

            if (wantSneak) {
                mc.thePlayer.movementInput.sneak       = true;
                mc.thePlayer.movementInput.moveStrafe  *= 0.3F;
                mc.thePlayer.movementInput.moveForward *= 0.3F;
            }
        } else if (mc.thePlayer.movementInput.sneak && !shouldSneak() && !isNearEdge()) {
            // Release sneak and start the unsneak cooldown so we don't instantly
            // re-sneak on the same tick (mirrors Raven's unsneakDelay).
            mc.thePlayer.movementInput.sneak = false;
            unsneakTicks = unsneakDelay.getValue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onDisabled() {
        sneakDelay   = 0;
        unsneakTicks = 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Property validation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void verifyValue(String name) {
        switch (name) {
            case "min-delay":
                if (minDelay.getValue() > maxDelay.getValue())
                    maxDelay.setValue(minDelay.getValue());
                break;
            case "max-delay":
                if (minDelay.getValue() > maxDelay.getValue())
                    minDelay.setValue(maxDelay.getValue());
                break;
            case "unsneak-delay":
                // clamp — IntProperty already handles min/max, nothing extra needed
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HUD suffix
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String[] getSuffix() {
        String delayStr = Objects.equals(minDelay.getValue(), maxDelay.getValue())
                ? minDelay.getValue().toString()
                : String.format("%d-%d", minDelay.getValue(), maxDelay.getValue());

        String edgeStr = edgeOffset.getValue() > 0.0f
                ? String.format(" | %.1fb", edgeOffset.getValue())
                : "";

        return new String[]{ delayStr + edgeStr };
    }
}
