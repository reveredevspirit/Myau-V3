package myau.module.modules;

import myau.module.Module;  // This is your base class – keep as-is

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.HashMap;
import java.util.Map;

public class BackTrack extends Module {

    private final Map<Integer, Vec3> realPositions = new HashMap<>();
    private Vec3 lastRealPos = null;
    private EntityLivingBase target = null;

    public BackTrack() {
        // Constructor – adjust parameters to match your actual Module base class.
        // Many simple clients use: super("Name", "Description")
        // If your Module has a category param, change to the correct one (e.g. ModuleCategory.COMBAT)
        // For now: assuming no category or it's optional/handled elsewhere
        super("BackTrack", "Tracks real server positions for better reach/backtrack");
        // If category is required, find the correct enum/class name in your project (search for "COMBAT" in other modules)
        // Example alternatives you might try:
        // super("BackTrack", "Tracks...", ModuleCategory.COMBAT);
        // super("BackTrack", Category.COMBAT, "Tracks...");
    }

    // Your packet handler – call this from wherever packets are processed (e.g. MixinNetworkManager or EventPacket)
    public void onPacket(Packet<?> packet) {
        if (!isEnabled()) return;  // Use your actual isEnabled() method name

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            Entity e = mc.theWorld.getEntityByID(p.getEntityId());  // Safer lookup

            if (e != null) {
                int id = e.getEntityId();
                Vec3 pos = new Vec3(e.posX, e.posY, e.posZ);

                pos = pos.addVector(
                        (double) p.getX() / 32.0,
                        (double) p.getY() / 32.0,
                        (double) p.getZ() / 32.0
                );

                realPositions.put(id, pos);
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;

            realPositions.put(
                    p.getEntityId(),
                    new Vec3(
                            (double) p.getX() / 32.0,
                            (double) p.getY() / 32.0,
                            (double) p.getZ() / 32.0
                    )
            );
        }
    }

    // Update/logic method – hook this to your update event if needed
    public void onUpdate() {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        // Simple target finder example (replace with your actual target logic, e.g. from KillAura)
        this.target = null;
        double closest = Double.MAX_VALUE;

        for (Object o : mc.theWorld.loadedEntityList) {
            if (o instanceof EntityLivingBase) {
                EntityLivingBase e = (EntityLivingBase) o;
                if (e != mc.thePlayer && !e.isDead) {
                    double dist = mc.thePlayer.getDistanceToEntity(e);
                    if (dist < closest) {
                        closest = dist;
                        this.target = e;
                    }
                }
            }
        }

        if (this.target != null) {
            Vec3 real = realPositions.get(this.target.getEntityId());

            if (real != null) {
                double distReal = mc.thePlayer.getDistanceSq(real.xCoord, real.yCoord, real.zCoord);
                double distCurrent = mc.thePlayer.getDistanceToEntity(this.target);

                if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
                    this.lastRealPos = real;
                }

                // Use distReal vs distCurrent for backtrack logic (e.g. extended reach check)
            }
        }
    }

    // Cleanup – no @Override, no super call (since base likely doesn't have onDisable())
    public void onDisable() {
        realPositions.clear();
        lastRealPos = null;
        target = null;
        // If your framework has onDisable logic, add it here without super
    }

    // Optional: if your Module base has onEnable(), add it similarly
    // public void onEnable() { ... }
}
