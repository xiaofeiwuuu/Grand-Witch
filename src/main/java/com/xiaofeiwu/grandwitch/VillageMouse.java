package com.xiaofeiwu.grandwitch;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * A child villager that the Grand Witch has turned into a mouse: small, quick, afraid of her and of cats. It keeps what the villager was (all of its
 * data), and is the villager again when the witch that did it dies, when it has had milk, or when its time is up.
 */
public class VillageMouse extends PathfinderMob {

    private CompoundTag original = new CompoundTag();
    private UUID maker;
    private long until;
    private boolean caged;

    public VillageMouse(EntityType<? extends VillageMouse> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12.0D).add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, GrandWitch.class, 12.0F, 1.4D, 1.8D));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Cat.class, 10.0F, 1.3D, 1.6D));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6.0F));
    }

    /** Made from this villager (or whatever it is), by this witch, for this long. */
    static VillageMouse from(ServerLevel level, Entity villager, UUID maker, int seconds) {
        VillageMouse mouse = ModEntities.VILLAGE_MOUSE.get().create(level);
        if (mouse == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        if (!villager.save(tag)) {
            return null;
        }
        tag.remove("UUID");
        mouse.original = tag;
        mouse.maker = maker;
        mouse.until = level.getGameTime() + seconds * 20L;
        mouse.moveTo(villager.getX(), villager.getY(), villager.getZ(), villager.getYRot(), 0.0F);
        return mouse;
    }

    /** Kept in the witch's cage: it does not go back to what it was by itself (someone has to let it out, and then to cure it). */
    public boolean isCaged() {
        return caged;
    }

    public void setCaged(boolean caged) {
        this.caged = caged;
        if (caged) {
            until = 0L;
        }
    }

    /** A wild mouse that a player has killed may have a plague virus on it; one that was somebody (a villager, a zombie) has nothing: it is they who are dead. */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit && original.isEmpty() && random.nextDouble() < WitchConfig.VIRUS_CHANCE.get() + 0.1D * looting) {
            spawnAtLocation(new net.minecraft.world.item.ItemStack(ModItems.PLAGUE_VIRUS.get()));
        }
    }

    public UUID maker() {
        return maker;
    }

    public boolean hasOriginal() {
        return !original.isEmpty();
    }

    /** It is the villager again, where it stands. */
    public boolean revert() {
        if (!(level() instanceof ServerLevel server) || isRemoved() || original.isEmpty()) {
            return false;
        }
        Optional<Entity> back = EntityType.create(original, server);
        if (back.isEmpty()) {
            return false;
        }
        Entity villager = back.get();
        villager.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        discard();
        server.addFreshEntity(villager);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY() + 0.3D, getZ(), 15, 0.2D, 0.2D, 0.2D, 0.03D);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && until > 0L && level().getGameTime() >= until) {
            revert();
        }
    }

    /** Milk, given to it, makes it a villager again. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.MILK_BUCKET)) {
            if (!level().isClientSide && revert() && !player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!WitchConfig.CHILD_MOUSE_CAN_DIE.get() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return original.isEmpty() && !caged;     // a child that was turned stays; a mouse from an egg is cleared like any other creature, unless it is kept
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Original", original);
        tag.putLong("Until", until);
        tag.putBoolean("Caged", caged);
        if (maker != null) {
            tag.putUUID("Maker", maker);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        original = tag.getCompound("Original");
        until = tag.getLong("Until");
        caged = tag.getBoolean("Caged");
        maker = tag.hasUUID("Maker") ? tag.getUUID("Maker") : null;
    }
}
