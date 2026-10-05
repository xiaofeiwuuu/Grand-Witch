package com.xiaofeiwu.grandwitch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * A broom that a player sits on and flies low over the ground: it goes where the rider looks, as fast as the broom is (the golden one is the faster), and keeps a
 * height above whatever is under it: it hovers a block or so up, higher if the rider looks up and lower if they look down, and no higher than the broom can. It is
 * the server that works out where it is (what the player's own side thinks of it is not asked), so that it cannot be out of step with the world. It wears as it is
 * used and breaks when it is worn out. Crouching gets off it; crouching and using it takes it up. A mouse cannot get on it. The witch rides one too, when she goes
 * after someone far off: one she calls up, that is gone when she gets off.
 */
public class BroomEntity extends Entity {

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BroomEntity.class, EntityDataSerializers.INT);

    private ItemStack item = ItemStack.EMPTY;
    private int flightTicks;
    /** Called up by the witch: it is gone when no one is on it. */
    private boolean summoned;

    /** Whether a living thing's jump key is held: the field is not open to us, and is read the way a mod reads one. */
    private static final Field JUMPING = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20899_");

    static boolean jumping(LivingEntity e) {
        try {
            return JUMPING.getBoolean(e);
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    void markSummoned() {
        summoned = true;
    }

    // for the other side: where it is to be, and in how many ticks
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;
    private int lerpSteps;

    public BroomEntity(EntityType<? extends BroomEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(KIND, 0);
    }

    public BroomKind kind() {
        return BroomKind.values()[Mth.clamp(entityData.get(KIND), 0, BroomKind.values().length - 1)];
    }

    public ItemStack item() {
        return item;
    }

    public void setItem(ItemStack stack) {
        this.item = stack;
        entityData.set(KIND, stack.getItem() instanceof BroomItem b ? b.kind().ordinal() : 0);
    }

    // ------------------------------------------------------------------ being on it ------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide && getPassengers().isEmpty()) {
                // taken up: back into the hand, worn as it is
                if (!player.getInventory().add(item.copy())) {
                    player.drop(item.copy(), false);
                }
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (Mice.isMouse(player)) {
            if (!level().isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.grandwitch.broom.mouse"), true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);          // a mouse cannot get up on it
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    /** The server alone says where it is: the side of the player does not steer it, and sends nothing of where it thinks it is. */
    @Override
    public boolean isControlledByLocalInstance() {
        return !level().isClientSide;
    }

    /** The rider sits on the handle: their feet a tenth of a block under the broom, which puts the seat of a person (three quarters of the way up them) on it. */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (hasPassenger(passenger)) {
            callback.accept(passenger, getX(), getY() - 0.1D, getZ());
        }
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return item.copy();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || level().isClientSide || isRemoved()) {
            return false;
        }
        spawnAtLocation(item.copy());
        discard();
        return true;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ------------------------------------------------------------------ flying ------------------------------------------------------------------

    /** How high above the ground under it the broom is: the top of the first thing a thing could stand on below, within 16 blocks; if there is none, as high as it is. */
    double groundY() {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = 0; dy <= 16; dy++) {
            cursor.set(getX(), getY() - dy, getZ());
            var state = level().getBlockState(cursor);
            if (state.blocksMotion() || !state.getFluidState().isEmpty()) {
                return cursor.getY() + 1.0D;
            }
        }
        return getY() - kind().hover;
    }

    /**
     * One tick of the server's working it out. {@code pilot} is who is on it, or null.
     * With a pilot: it turns to where they look, goes as they press (forward, back, to the side), at the speed of its kind, and keeps to a height that looks
     * decide: level, the hover; up, as high as the kind can go; down, as low as half a block. With none: it settles to the ground.
     */
    void step(LivingEntity rider) {
        Vec3 v = getDeltaMovement();
        double ground = groundY();
        boolean moving = false;
        BroomKind kind = kind();
        Player pilot = rider instanceof Player p ? p : null;
        if (pilot != null) {
            float yaw = pilot.getYRot();
            setYRot(yaw);
            yRotO = yaw;
            double forward = pilot.zza, side = pilot.xxa;
            double length = Math.sqrt(forward * forward + side * side);
            if (length > 1.0D) {
                forward /= length;
                side /= length;
            }
            moving = length > 0.01D;
            double rad = yaw * Mth.DEG_TO_RAD;
            double tx = (-Math.sin(rad) * forward + Math.cos(rad) * side) * kind.speed;
            double tz = (Math.cos(rad) * forward + Math.sin(rad) * side) * kind.speed;
            // the height: the jump key climbs (to the most the broom can), and what is looked at steers it when it is not held: up, higher, down, lower
            double look = Mth.clamp(-pilot.getXRot() / 45.0D, -1.0D, 1.0D);
            double altitude = look >= 0.0D ? kind.hover + look * (kind.maxAltitude - kind.hover) : kind.hover + look * (kind.hover - 0.5D);
            if (jumping(pilot)) {
                altitude = kind.maxAltitude;
                moving = true;
            }
            double vy = Mth.clamp((ground + altitude - getY()) * 0.25D, -0.2D, 0.25D);
            if (horizontalCollision) {
                vy = Math.max(vy, 0.25D);                          // a wall in the way: over it
            }
            if (getY() - ground > kind.maxAltitude) {
                vy = Math.min(vy, 0.0D);
            }
            v = new Vec3(Mth.lerp(0.2D, v.x, tx), vy, Mth.lerp(0.2D, v.z, tz));
        } else if (rider instanceof GrandWitch witch && witch.flyTarget() != null) {
            // the witch's: toward whom she is after, low, and over what is in the way
            Vec3 target = witch.flyTarget();
            double dx = target.x - getX(), dz = target.z - getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            double speed = WitchConfig.BROOM_SPEED.get();
            double want = Math.min(Math.max(ground + 1.4D, target.y + 0.3D), ground + 4.0D);
            double vy = Mth.clamp((want - getY()) * 0.25D, -0.2D, 0.25D);
            if (horizontalCollision) {
                vy = Math.max(vy, 0.3D);
            }
            v = new Vec3(flat > 0.5D ? dx / flat * speed : 0.0D, vy, flat > 0.5D ? dz / flat * speed : 0.0D);
            if (flat > 0.5D) {
                float yaw = (float) (Mth.atan2(dz, dx) * 57.29577951308232D) - 90.0F;
                setYRot(yaw);
                yRotO = yaw;
                witch.setYRot(yaw);
                witch.yBodyRot = yaw;
                witch.yHeadRot = yaw;
            }
        } else {
            v = new Vec3(v.x * 0.8D, Mth.clamp((ground + 0.05D - getY()) * 0.25D, -0.2D, 0.1D), v.z * 0.8D);
        }
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        if (rider != null && rider.getVehicle() == this) {
            rider.fallDistance = 0.0F;
        }
        if (pilot != null && moving && ++flightTicks % 20 == 0 && level() instanceof ServerLevel server) {
            if (item.hurt(1, random, null)) {
                // worn out: it falls to bits under them
                server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.3D, getZ(), 20, 0.4D, 0.2D, 0.4D, 0.03D);
                server.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
                pilot.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.grandwitch.broom.broke"), true);
                ejectPassengers();
                discard();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                double d = 1.0D / lerpSteps;
                setPos(getX() + (lerpX - getX()) * d, getY() + (lerpY - getY()) * d, getZ() + (lerpZ - getZ()) * d);
                setYRot(getYRot() + Mth.wrapDegrees(lerpYRot - getYRot()) * (float) d);
                lerpSteps--;
            }
            return;
        }
        LivingEntity rider = getControllingPassenger();
        if (summoned && rider == null) {
            discard();                                        // the witch's, and she has got off
            return;
        }
        step(rider);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpSteps = Math.max(2, steps);
    }

    // ------------------------------------------------------------------ keeping ------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", item.save(new CompoundTag()));
        tag.putBoolean("Summoned", summoned);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setItem(ItemStack.of(tag.getCompound("Item")));
        summoned = tag.getBoolean("Summoned");
    }
}
