package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.BooleanSetting;
import myau.module.Module;
import myau.module.SliderSetting;
import myau.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AimAssist extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();

    public final SliderSetting  hSpeed     = new SliderSetting("H-Speed",   3.0, 0.0, 10.0, 0.1);
    public final SliderSetting  vSpeed     = new SliderSetting("V-Speed",   0.0, 0.0, 10.0, 0.1);
    public final SliderSetting  smoothing  = new SliderSetting("Smoothing", 50,  0,   100,   1);
    public final SliderSetting  range      = new SliderSetting("Range",     4.5, 3.0, 8.0,  0.1);
    public final SliderSetting  fov        = new SliderSetting("FOV",       90,  30,  360,   1);
    public final BooleanSetting weaponOnly = new BooleanSetting("Weapons Only", true);
    public final BooleanSetting allowTools = new BooleanSetting("Allow Tools",  false);
    public final BooleanSetting botChecks  = new BooleanSetting("Bot Check",    true);
    public final BooleanSetting team       = new BooleanSetting("Teams",        true);

    public AimAssist() {
        super("AimAssist", false);
        register(hSpeed, vSpeed, smoothing, range, fov, weaponOnly, allowTools, botChecks, team);
    }

    private boolean isValidTarget(EntityPlayer entityPlayer) {
        if (entityPlayer == mc.thePlayer || entityPlayer == mc.thePlayer.ridingEntity) return false;
        if (entityPlayer == mc.getRenderViewEntity() || entityPlayer == mc.getRenderViewEntity().ridingEntity) return false;
        if (entityPlayer.deathTime > 0) return false;
        if (RotationUtil.distanceToEntity(entityPlayer) > range.getValue()) return false;
        if (RotationUtil.angleToEntity(entityPlayer) > (float) fov.getValue()) return false;
        if (RotationUtil.rayTrace(entityPlayer) != null) return false;
        if (TeamUtil.isFriend(entityPlayer)) return false;
        return (!team.getValue() || !TeamUtil.isSameTeam(entityPlayer))
                && (!botChecks.getValue() || !TeamUtil.isBot(entityPlayer));
    }

    private boolean isInReach(EntityPlayer entityPlayer) {
        Reach reach = (Reach) Myau.moduleManager.modules.get(Reach.class);
        double distance = reach.isEnabled() ? reach.range.getValue() : 3.0;
        return RotationUtil.distanceToEntity(entityPlayer) <= distance;
    }

    private boolean isLookingAtBlock() {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST && mc.currentScreen == null) {
            if (!weaponOnly.getValue() || ItemUtil.hasRawUnbreakingEnchant()
                    || allowTools.getValue() && ItemUtil.isHoldingTool()) {
                boolean attacking = PlayerUtil.isAttacking();
                if (!attacking || !isLookingAtBlock()) {
                    if (attacking || !timer.hasTimeElapsed(350L)) {
                        List<EntityPlayer> inRange = mc.theWorld.loadedEntityList.stream()
                                .filter(e -> e instanceof EntityPlayer)
                                .map(e -> (EntityPlayer) e)
                                .filter(this::isValidTarget)
                                .sorted(Comparator.comparingDouble(RotationUtil::distanceToEntity))
                                .collect(Collectors.toList());
                        if (!inRange.isEmpty()) {
                            if (inRange.stream().anyMatch(this::isInReach))
                                inRange.removeIf(p -> !isInReach(p));
                            EntityPlayer player = inRange.get(0);
                            if (RotationUtil.distanceToEntity(player) > 0.0) {
                                AxisAlignedBB bb = player.getEntityBoundingBox();
                                float border = player.getCollisionBorderSize();
                                float[] rotation = RotationUtil.getRotationsToBox(
                                        bb.expand(border, border, border),
                                        mc.thePlayer.rotationYaw,
                                        mc.thePlayer.rotationPitch,
                                        180.0F,
                                        (float) smoothing.getValue() / 100.0F);
                                float yaw   = Math.min((float) Math.abs(hSpeed.getValue()), 10.0F);
                                float pitch = Math.min((float) Math.abs(vSpeed.getValue()), 10.0F);
                                Myau.rotationManager.setRotation(
                                        mc.thePlayer.rotationYaw   + (rotation[0] - mc.thePlayer.rotationYaw)   * 0.1F * yaw,
                                        mc.thePlayer.rotationPitch + (rotation[1] - mc.thePlayer.rotationPitch) * 0.1F * pitch,
                                        0, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPress(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindAttack.getKeyCode()
                && !Myau.moduleManager.modules.get(AutoClicker.class).isEnabled()) {
            timer.reset();
        }
    }
}
