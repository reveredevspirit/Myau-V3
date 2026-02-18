package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.UpdateEvent;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class Autoblock extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode;
    public final BooleanProperty requirePress;
    public final BooleanProperty requireAttack;
    public final FloatProperty blockRange;
    public final FloatProperty minCPS;
    public final FloatProperty maxCPS;

    private boolean blockingState = false;
    private boolean fakeBlockState = false;
    private boolean isBlocking = false;
    private boolean blinkReset = false;
    private int blockTick = 0;
    private long blockDelayMS = 0L;

    public Autoblock() {
        super("Autoblock", false);

        this.mode = new ModeProperty(
                "mode",
                0,
                new String[]{
                        "NONE",
                        "VANILLA",
                        "SPOOF",
                        "HYPIXEL",
                        "BLINK",
                        "INTERACT",
                        "SWAP",
                        "LEGIT",
                        "FAKE",
                        "LAGRANGE"
                }
        );

        this.requirePress = new BooleanProperty("require-press", false);
        this.requireAttack = new BooleanProperty("require-attack", false);
        this.blockRange = new FloatProperty("block-range", 6.0F, 3.0F, 8.0F);
        this.minCPS = new FloatProperty("min-aps", 8.0F, 1.0F, 20.0F);
        this.maxCPS = new FloatProperty("max-aps", 10.0F, 1.0F, 20.0F);
    }

    private long getBlockDelay() {
        return (long) (1000.0F / RandomUtil.nextLong(
                this.minCPS.getValue().longValue(),
                this.maxCPS.getValue().longValue()
        ));
    }

    private boolean canAutoblock() {
        if (!ItemUtil.isHoldingSword()) return false;
        if (this.requirePress.getValue() && !PlayerUtil.isUsingItem()) return false;

        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (this.requireAttack.getValue() && !ka.isAttackAllowed()) return false;

        return true;
    }

    private boolean hasValidTarget() {
        return mc.theWorld.loadedEntityList.stream().anyMatch(
                entity -> entity instanceof net.minecraft.entity.EntityLivingBase
                        && RotationUtil.distanceToEntity((net.minecraft.entity.EntityLivingBase) entity)
                        <= this.blockRange.getValue()
        );
    }
