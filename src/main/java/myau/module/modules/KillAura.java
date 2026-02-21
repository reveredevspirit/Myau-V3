package myau.module.modules;

import myau.Myau;
import myau.event.EventManager;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.*;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.BooleanSetting;
import myau.module.DropdownSetting;
import myau.module.Module;
import myau.module.SliderSetting;
import myau.util.*;
import myau.util.AttackData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;

public class KillAura extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));

    private final TimerUtil timer = new TimerUtil();
    private AttackData target = null;
    private boolean hitRegistered = false;
    private long attackDelayMS = 0L;

    // Mode
    public final DropdownSetting mode          = new DropdownSetting("Mode",        0, "SINGLE", "SWITCH");
    public final DropdownSetting sort          = new DropdownSetting("Sort",        0, "DISTANCE", "HEALTH", "HURT_TIME", "FOV");

    // Range
    public final SliderSetting swingRange      = new SliderSetting("Swing Range",  3.5, 3.0, 6.0, 0.1);
    public final SliderSetting attackRange     = new SliderSetting("Attack Range", 3.0, 3.0, 6.0, 0.1);
    public final SliderSetting fov             = new SliderSetting("FOV",          360,  30, 360,   1);

    // CPS
    public final SliderSetting minCPS          = new SliderSetting("Min APS",       14,   1,  20,   1);
    public final SliderSetting maxCPS          = new SliderSetting("Max APS",       14,   1,  20,   1);
    public final SliderSetting switchDelay     = new SliderSetting("Switch Delay", 150,   0, 1000, 10);

    // Rotations
    public final DropdownSetting rotations     = new DropdownSetting("Rotations",  2, "NONE", "LEGIT", "SILENT", "LOCK_VIEW");
    public final DropdownSetting moveFix       = new DropdownSetting("Move Fix",   1, "NONE", "SILENT", "STRICT");
    public final SliderSetting smoothing       = new SliderSetting("Smoothing",    0,   0, 100,   1);
    public final SliderSetting angleStep       = new SliderSetting("Angle Step",  90,  30, 180,   1);

    // Behaviour
    public final BooleanSetting throughWalls   = new BooleanSetting("Through Walls",  true);
    public final BooleanSetting requirePress   = new BooleanSetting("Require Press",  false);
    public final BooleanSetting allowMining    = new BooleanSetting("Allow Mining",   true);
    public final BooleanSetting weaponsOnly    = new BooleanSetting("Weapons Only",   true);
    public final BooleanSetting allowTools     = new BooleanSetting("Allow Tools",    false);
    public final BooleanSetting inventoryCheck = new BooleanSetting("Inv Check",      true);
    public final BooleanSetting botCheck       = new BooleanSetting("Bot Check",      true);

    // Targets
    public final BooleanSetting players        = new BooleanSetting("Players",    true);
    public final BooleanSetting bosses         = new BooleanSetting("Bosses",     false);
    public final BooleanSetting mobs           = new BooleanSetting("Mobs",       false);
    public final BooleanSetting animals        = new BooleanSetting("Animals",    false);
    public final BooleanSetting golems         = new BooleanSetting("Golems",     false);
    public final BooleanSetting silverfish     = new BooleanSetting("Silverfish", false);
    public final BooleanSetting teams          = new BooleanSetting("Teams",      true);

    // Display
    public final DropdownSetting showTarget    = new DropdownSetting("Show Target", 0, "NONE", "DEFAULT", "HUD");
    public final DropdownSetting debugLog      = new DropdownSetting("Debug Log",   0, "NONE", "HEALTH");

    public KillAura() {
        super("KillAura", false);
        register(mode);
        register(sort);
        register(swingRange);
        register(attackRange);
        register(fov);
        register(minCPS);
        register(maxCPS);
        register(switchDelay);
        register(rotations);
        register(moveFix);
        register(smoothing);
        register(angleStep);
        register(throughWalls);
        register(requirePress);
        register(allowMining);
        register(weaponsOnly);
        register(allowTools);
        register(inventoryCheck);
        register(botCheck);
        register(players);
        register(bosses);
        register(mobs);
        register(animals);
        register(golems);
        register(silverfish);
        register(teams);
        register(showTarget);
        register(debugLog);
    }

    private long getAttackDelay() {
        return 1000L / RandomUtil.nextLong((long) minCPS.getValue(), (long) maxCPS.getValue());
    }

    public EntityLivingBase getTarget() {
        return target != null ? target.getEntity() : null;
    }

    public boolean isAttackAllowed() {
        Scaffold scaffold = (Scaffold) Myau.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) return false;
        if (!weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || allowTools.getValue() && ItemUtil.isHoldingTool()) {
            return !requirePress.getValue()
                    || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
        }
        return false;
    }

    private boolean canAttack() {
        if (inventoryCheck.getValue() && mc.currentScreen instanceof GuiContainer) return false;

        if (!weaponsOnly.getValue()
                || ItemUtil.hasRawUnbreakingEnchant()
                || allowTools.getValue() && ItemUtil.isHoldingTool()) {

            if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) return false;
            if ((ItemUtil.isEating() || ItemUtil.isUsingBow()) && PlayerUtil.isUsingItem()) return false;

            AutoHeal autoHeal = (AutoHeal) Myau.moduleManager.modules.get(AutoHeal.class);
            if (autoHeal.isEnabled() && autoHeal.isSwitching()) return false;

            BedNuker bedNuker = (BedNuker) Myau.moduleManager.modules.get(BedNuker.class);
            if (bedNuker.isEnabled() && bedNuker.isReady()) return false;

            if (Myau.moduleManager.modules.get(Scaffold.class).isEnabled()) return false;

            if (requirePress.getValue()) {
                return PlayerUtil.isAttacking();
            } else {
                return !allowMining.getValue()
                        || !mc.objectMouseOver.typeOfHit.equals(MovingObjectType.BLOCK)
                        || !PlayerUtil.isAttacking();
            }
        }
        return false;
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (!mc.theWorld.loadedEntityList.contains(entity)) return false;
        if (entity == mc.thePlayer || entity == mc.thePlayer.ridingEntity) return false;
        if (entity == mc.getRenderViewEntity() || entity == mc.getRenderViewEntity().ridingEntity) return false;
        if (entity.deathTime > 0) return false;
        if (RotationUtil.angleToEntity(entity) > (float) fov.getValue()) return false;
        if (!throughWalls.getValue() && RotationUtil.rayTrace(entity) != null) return false;

        if (entity instanceof EntityOtherPlayerMP) {
            if (!players.getValue()) return false;
            if (TeamUtil.isFriend((EntityPlayer) entity)) return false;
            if (teams.getValue() && TeamUtil.isSameTeam((EntityPlayer) entity)) return false;
            if (botCheck.getValue() && TeamUtil.isBot((EntityPlayer) entity)) return false;
            return true;
        }

        if (entity instanceof EntityDragon || entity instanceof EntityWither)
            return bosses.getValue();

        if (entity instanceof EntityMob || entity instanceof EntitySlime) {
            if (entity instanceof EntitySilverfish)
                return silverfish.getValue() && (!teams.getValue() || !TeamUtil.hasTeamColor(entity));
            return mobs.getValue();
        }

        if (entity instanceof EntityAnimal
                || entity instanceof EntityBat
                || entity instanceof EntitySquid
                || entity instanceof EntityVillager)
            return animals.getValue();

        if (entity instanceof EntityIronGolem)
            return golems.getValue() && (!teams.getValue() || !TeamUtil.hasTeamColor(entity));

        return false;
    }

    private boolean isBoxInSwingRange(AxisAlignedBB box) {
        return RotationUtil.distanceToBox(box) <= swingRange.getValue();
    }

    private boolean isBoxInAttackRange(AxisAlignedBB box) {
        return RotationUtil.distanceToBox(box) <= attackRange.getValue();
    }

    private boolean performAttack(float yaw, float pitch) {
        if (Myau.playerStateManager.digging || Myau.playerStateManager.placing) return false;
        if (attackDelayMS > 0L) return false;

        // Stop autoblock before sending attack to prevent multiaction packets
        // Autoblock re-blocks automatically on the next tick
        Autoblock autoblock = (Autoblock) Myau.moduleManager.modules.get(Autoblock.class);
        if (autoblock != null && autoblock.isEnabled() && autoblock.isPlayerBlocking()) {
            autoblock.stopBlock();
        }

        attackDelayMS += getAttackDelay();
        mc.thePlayer.swingItem();

        if ((rotations.getIndex() != 0 || !isBoxInAttackRange(target.getBox()))
                && RotationUtil.rayTrace(target.getBox(), yaw, pitch, (float) attackRange.getValue()) == null) {
            return false;
        }

        AttackEvent event = new AttackEvent(target.getEntity());
        EventManager.call(event);

        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
        PacketUtil.sendPacket(new C02PacketUseEntity(target.getEntity(), Action.ATTACK));

        if (mc.playerController.getCurrentGameType() != GameType.SPECTATOR)
            PlayerUtil.attackEntity(target.getEntity());

        hitRegistered = true;
        return true;
    }

    @EventTarget(Priority.LOW)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || event.getType() != EventType.PRE) return;

        if (attackDelayMS > 0L) attackDelayMS -= 50L;

        if (target != null && canAttack() && isBoxInSwingRange(target.getBox())) {
            if (rotations.getIndex() == 2 || rotations.getIndex() == 3) {
                float[] rots = RotationUtil.getRotationsToBox(
                        target.getBox(),
                        event.getYaw(),
                        event.getPitch(),
                        (float) angleStep.getValue() + RandomUtil.nextFloat(-5.0F, 5.0F),
                        (float) smoothing.getValue() / 100.0F);

                event.setRotation(rots[0], rots[1], 1);

                if (rotations.getIndex() == 3)
                    Myau.rotationManager.setRotation(rots[0], rots[1], 1, true);

                if (moveFix.getIndex() != 0 || rotations.getIndex() == 3)
                    event.setPervRotation(rots[0], 1);
            }

            performAttack(event.getNewYaw(), event.getNewPitch());
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;
        if (event.getType() == EventType.PRE) {
            if (target == null
                    || !isValidTarget(target.getEntity())
                    || !isBoxInAttackRange(target.getBox())
                    || !isBoxInSwingRange(target.getBox())
                    || timer.hasTimeElapsed((long) switchDelay.getValue())) {
                target = findTarget();
                timer.reset();
            }
        }
    }

    private AttackData findTarget() {
        ArrayList<AttackData> targets = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) entity;
            if (!isValidTarget(living)) continue;
            targets.add(new AttackData(living));
        }
        if (targets.isEmpty()) return null;

        switch (sort.getIndex()) {
            case 0: targets.sort((a, b) -> Double.compare(RotationUtil.distanceToBox(a.getBox()), RotationUtil.distanceToBox(b.getBox()))); break;
            case 1: targets.sort((a, b) -> Float.compare(a.getEntity().getHealth(), b.getEntity().getHealth())); break;
            case 2: targets.sort((a, b) -> Integer.compare(a.getEntity().hurtTime, b.getEntity().hurtTime)); break;
            case 3: targets.sort((a, b) -> Float.compare(RotationUtil.angleToEntity(a.getEntity()), RotationUtil.angleToEntity(b.getEntity()))); break;
        }
        return targets.get(0);
    }
}
