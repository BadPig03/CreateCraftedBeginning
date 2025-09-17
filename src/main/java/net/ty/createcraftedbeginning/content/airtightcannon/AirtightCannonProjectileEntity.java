package net.ty.createcraftedbeginning.content.airtightcannon;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.particle.AirParticleData;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileRenderMode;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileType;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirtightCannonProjectileEntity extends AbstractHurtingProjectile implements IEntityWithComplexSpawn {
    protected AirtightCannonProjectileType type;
    protected ItemStack stack = ItemStack.EMPTY;

    protected Entity stuckEntity;
    protected Vec3 stuckOffset;
    protected AirtightCannonProjectileRenderMode stuckRenderer;
    protected double stuckFallSpeed;

    protected float additionalDamageMultiplier = 1;
    protected float additionalKnockback = 0;
    protected float recoveryChance = 0;

    public AirtightCannonProjectileEntity(EntityType<? extends AbstractHurtingProjectile> type, Level level) {
        super(type, level);
    }

    public static void playHitSound(Level world, Vec3 location) {
        AllSoundEvents.POTATO_HIT.playOnServer(world, BlockPos.containing(location));
    }

    public static void playLaunchSound(Level world, Vec3 location, float pitch) {
        AllSoundEvents.FWOOMP.playAt(world, location, 1, pitch, true);
    }

    @SuppressWarnings("unchecked")
    public static EntityType.@NotNull Builder<?> build(EntityType.Builder<?> builder) {
        EntityType.Builder<AirtightCannonProjectileEntity> entityBuilder = (EntityType.Builder<AirtightCannonProjectileEntity>) builder;
        return entityBuilder.sized(.25f, .25f);
    }

    public void setEnchantmentEffectsFromCannon(@NotNull ItemStack cannon) {
        Registry<Enchantment> enchantmentRegistry = registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        int recovery = cannon.getEnchantmentLevel(enchantmentRegistry.getHolderOrThrow(Enchantments.INFINITY));
        if (recovery > 0) {
            recoveryChance = .125f + recovery * .125f;
        }
    }

    public ItemStack getItem() {
        return stack;
    }

    public void setItem(@NotNull ItemStack stack) {
        this.stack = stack;
        type = AirtightCannonProjectileType.getTypeForItem(level().registryAccess(), stack.getItem()).orElseGet(() -> level().registryAccess().registryOrThrow(CCBRegistries.AIRTIGHT_CANNON_PROJECTILE_TYPE).getHolderOrThrow(CCBAirtightCannonProjectileTypes.FALLBACK)).value();
    }

    @Nullable
    public AirtightCannonProjectileType getProjectileType() {
        return type;
    }

    @Nullable
    public Entity getStuckEntity() {
        if (stuckEntity == null) {
            return null;
        }
        if (!stuckEntity.isAlive()) {
            return null;
        }
        return stuckEntity;
    }

    public void setStuckEntity(@NotNull Entity stuckEntity) {
        this.stuckEntity = stuckEntity;
        this.stuckOffset = position().subtract(stuckEntity.position());
        this.stuckRenderer = new CCBAirtightCannonProjectileRenderModes.StuckToEntity(stuckOffset);
        this.stuckFallSpeed = 0.0;
        setDeltaMovement(Vec3.ZERO);
    }

    public AirtightCannonProjectileRenderMode getRenderMode() {
        if (getStuckEntity() != null) {
            return stuckRenderer;
        }

        return type.renderMode();
    }

    @Override
    public void tick() {
        Entity stuckEntity = getStuckEntity();
        if (stuckEntity != null) {
            if (getY() < stuckEntity.getY() - 0.1) {
                pop(position());
                kill();
            } else {
                stuckFallSpeed += 0.007 * type.gravityMultiplier();
                stuckOffset = stuckOffset.add(0, -stuckFallSpeed, 0);
                Vec3 pos = stuckEntity.position().add(stuckOffset);
                setPos(pos.x, pos.y, pos.z);
            }
        } else {
            setDeltaMovement(getDeltaMovement().add(0, -0.05 * type.gravityMultiplier(), 0).scale(type.drag()));
        }

        super.tick();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amt) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        pop(position());
        kill();
        return true;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return new AirParticleData(1, 10);
    }

    @Override
    protected float getInertia() {
        return 1;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        compoundTag.put("Item", stack.saveOptional(this.registryAccess()));
        compoundTag.putFloat("AdditionalDamage", additionalDamageMultiplier);
        compoundTag.putFloat("AdditionalKnockback", additionalKnockback);
        compoundTag.putFloat("Recovery", recoveryChance);
        super.addAdditionalSaveData(compoundTag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        setItem(ItemStack.parseOptional(this.registryAccess(), compoundTag.getCompound("Item")));
        additionalDamageMultiplier = compoundTag.getFloat("AdditionalDamage");
        additionalKnockback = compoundTag.getFloat("AdditionalKnockback");
        recoveryChance = compoundTag.getFloat("Recovery");
        super.readAdditionalSaveData(compoundTag);
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult ray) {
        super.onHitEntity(ray);

        if (getStuckEntity() != null) {
            return;
        }

        Vec3 hit = ray.getLocation();
        Entity target = ray.getEntity();
        float damage = type.damage() * additionalDamageMultiplier;
        float knockback = type.knockback() + additionalKnockback;
        Entity owner = this.getOwner();

        if (!target.isAlive()) {
            return;
        }
        if (owner instanceof LivingEntity) {
            ((LivingEntity) owner).setLastHurtMob(target);
        }

        if (target instanceof AirtightCannonProjectileEntity) {
            if (tickCount < 10 && target.tickCount < 10) {
                return;
            }
        }

        pop(hit);

        if (target instanceof WitherBoss && ((WitherBoss) target).isPowered()) {
            return;
        }
        if (type.preEntityHit(stack, ray)) {
            return;
        }

        boolean targetIsEnderman = target.getType() == EntityType.ENDERMAN;
        int k = target.getRemainingFireTicks();
        if (this.isOnFire() && !targetIsEnderman) {
            target.igniteForSeconds(5);
        }

        boolean onServer = !level().isClientSide;
        DamageSource damageSource = causePotatoDamage();
        if (onServer && !target.hurt(damageSource, damage)) {
            target.setRemainingFireTicks(k);
            kill();
            return;
        }

        if (targetIsEnderman) {
            return;
        }

        if (!type.onEntityHit(stack, ray) && onServer) {
            if (random.nextDouble() <= recoveryChance) {
                recoverItem();
            } else {
                spawnAtLocation(type.dropStack());
            }
        }

        if (!(target instanceof LivingEntity livingentity)) {
            playHitSound(level(), position());
            kill();
            return;
        }

        if (type.reloadTicks() < 10) {
            livingentity.invulnerableTime = type.reloadTicks() + 10;
        }

        if (onServer && knockback > 0) {
            Vec3 appliedMotion = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale(knockback * 0.6);
            if (appliedMotion.lengthSqr() > 0.0D) {
                livingentity.push(appliedMotion.x, 0.1D, appliedMotion.z);
            }
        }

        if (onServer && owner instanceof LivingEntity) {
            EnchantmentHelper.doPostAttackEffects((ServerLevel) level(), livingentity, damageSource);
        }

        if (livingentity != owner && livingentity instanceof Player && owner instanceof ServerPlayer && !this.isSilent()) {
            ((ServerPlayer) owner).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0f));
        }

        if (type.sticky() && target.isAlive()) {
            setStuckEntity(target);
        } else {
            kill();
        }
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult ray) {
        Vec3 hit = ray.getLocation();
        pop(hit);
        if (!type.onBlockHit(level(), stack, ray) && !level().isClientSide) {
            if (random.nextDouble() <= recoveryChance) {
                recoverItem();
            } else if (getProjectileType() != null) {
                spawnAtLocation(getProjectileType().dropStack());
            }
        }

        super.onHitBlock(ray);
        kill();
    }

    private void recoverItem() {
        if (!stack.isEmpty()) {
            spawnAtLocation(stack.copyWithCount(1));
        }
    }

    private void pop(Vec3 hit) {
        if (!stack.isEmpty()) {
            for (int i = 0; i < 7; i++) {
                Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, this.random, .25f);
                level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), hit.x, hit.y, hit.z, m.x, m.y, m.z);
            }
        }
        if (!level().isClientSide) {
            playHitSound(level(), position());
        }
    }

    @Contract(" -> new")
    private @NotNull DamageSource causePotatoDamage() {
        return CreateDamageSources.potatoCannon(level(), this, getOwner());
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        CompoundTag compound = new CompoundTag();
        addAdditionalSaveData(compound);
        buffer.writeNbt(compound);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf additionalData) {
        CompoundTag nbt = additionalData.readNbt();
        if (nbt != null) {
            readAdditionalSaveData(nbt);
        }
    }
}
