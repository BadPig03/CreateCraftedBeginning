package net.ty.createcraftedbeginning.content.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.GlobalRegistryAccess;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileType;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.compressedair.CanisterUtil;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class AirtightCannonItem extends ProjectileWeaponItem implements CustomArmPoseItem {
    public AirtightCannonItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static Ammo getAmmo(@NotNull Player player, ItemStack heldStack) {
        ItemStack ammoStack = player.getProjectile(heldStack);
        if (ammoStack.isEmpty()) {
            return null;
        }

        Optional<Holder.Reference<AirtightCannonProjectileType>> optionalType = AirtightCannonProjectileType.getTypeForItem(player.level().registryAccess(), ammoStack.getItem());
        return optionalType.map(reference -> new Ammo(ammoStack, reference.value())).orElse(null);
    }

    private static int maxUses() {
        return CCBConfig.server().equipment.maxAirtightCannonShots.get();
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player) {
        return false;
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity miningEntity) {
        return false;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        if (context.getPlayer() != null) {
            return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
        }
        return InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (ShootableGadgetItemMethods.shouldSwap(player, heldStack, hand, s -> s.getItem() instanceof AirtightCannonItem)) {
            return InteractionResultHolder.fail(heldStack);
        }

        Ammo ammo = getAmmo(player, heldStack);
        if (ammo == null) {
            return InteractionResultHolder.pass(heldStack);
        }

        ItemStack ammoStack = ammo.stack();
        AirtightCannonProjectileType projectileType = ammo.type();

        if (level.isClientSide) {
            CreateCraftedBeginningClient.AIRTIGHT_CANNON_RENDER_HANDLER.dontAnimateItem(hand);
            return InteractionResultHolder.success(heldStack);
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 barrelPos = eyePos.add(lookVec.scale(0.75));
        Vec3 motion = lookVec.normalize().scale(2).scale(projectileType.velocityMultiplier());

        float soundPitch = projectileType.soundPitch() + (level.getRandom().nextFloat() - .5f) / 4f;
        boolean spray = projectileType.split() > 1;
        Vec3 sprayBase = VecHelper.rotate(new Vec3(0, 0.1, 0), 360 * level.getRandom().nextFloat(), Direction.Axis.Z);
        float sprayChange = 360f / projectileType.split();

        ItemStack ammoStackCopy = ammoStack.copy();

        for (int i = 0; i < projectileType.split(); i++) {
            AirtightCannonProjectileEntity projectile = CCBEntityTypes.AIRTIGHT_CANNON_PROJECTILE.create(level);
            if (projectile == null) {
                continue;
            }
            projectile.setItem(ammoStackCopy);
            projectile.setEnchantmentEffectsFromCannon(heldStack);

            Vec3 splitMotion = motion;
            if (spray) {
                float imperfection = 40 * (level.getRandom().nextFloat() - 0.5f);
                Vec3 sprayOffset = VecHelper.rotate(sprayBase, i * sprayChange + imperfection, Direction.Axis.Z);
                splitMotion = splitMotion.add(VecHelper.lookAt(sprayOffset, motion));
            }

            if (i != 0) {
                projectile.recoveryChance = 0;
            }

            projectile.setPos(barrelPos.x, barrelPos.y, barrelPos.z);
            projectile.setDeltaMovement(splitMotion);
            projectile.setOwner(player);
            level.addFreshEntity(projectile);
        }

        if (!player.isCreative()) {
            ammoStack.shrink(1);
            if (ammoStack.isEmpty()) {
                player.getInventory().removeItem(ammoStack);
            }
        }

        if (!CanisterUtil.canAbsorbDamage(player, maxUses())) {
            heldStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }

        ShootableGadgetItemMethods.applyCooldown(player, heldStack, hand, s -> s.getItem() instanceof AirtightCannonItem, projectileType.reloadTicks());
        ShootableGadgetItemMethods.sendPackets(player, b -> new AirtightCannonPacket(barrelPos, lookVec.normalize(), ammoStack, hand, soundPitch, b));
        return InteractionResultHolder.success(heldStack);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return CanisterUtil.isBarVisible(stack, maxUses());
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return CanisterUtil.getBarWidth(stack, maxUses());
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return CanisterUtil.getBarColor(stack, maxUses());
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }

        Ammo ammo = getAmmo(player, stack);
        if (ammo == null) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }
        ItemStack ammoStack = ammo.stack();
        AirtightCannonProjectileType type = ammo.type();

        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return;
        }

        HolderLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        int power = stack.getEnchantmentLevel(lookup.getOrThrow(Enchantments.POWER));
        int punch = stack.getEnchantmentLevel(lookup.getOrThrow(Enchantments.PUNCH));
        final float additionalDamageMultiplier = 1 + power * .2f;
        final float additionalKnockback = punch * .5f;

        String _attack = "potato_cannon.ammo.attack_damage";
        String _reload = "potato_cannon.ammo.reload_ticks";
        String _knockback = "potato_cannon.ammo.knockback";

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(Component.translatable(ammoStack.getDescriptionId()).append(Component.literal(":")).withStyle(ChatFormatting.GRAY));
        MutableComponent spacing = CommonComponents.space();
        ChatFormatting green = ChatFormatting.GREEN;
        ChatFormatting darkGreen = ChatFormatting.DARK_GREEN;

        float damageF = type.damage() * additionalDamageMultiplier;
        MutableComponent damage = Component.literal(damageF == Mth.floor(damageF) ? "" + Mth.floor(damageF) : "" + damageF);
        MutableComponent reloadTicks = Component.literal("" + type.reloadTicks());
        MutableComponent knockback = Component.literal("" + (type.knockback() + additionalKnockback));

        damage = damage.withStyle(additionalDamageMultiplier > 1 ? green : darkGreen);
        knockback = knockback.withStyle(additionalKnockback > 0 ? green : darkGreen);
        reloadTicks = reloadTicks.withStyle(darkGreen);

        tooltip.add(spacing.plainCopy().append(CreateLang.translateDirect(_attack, damage).withStyle(darkGreen)));
        tooltip.add(spacing.plainCopy().append(CreateLang.translateDirect(_reload, reloadTicks).withStyle(darkGreen)));
        tooltip.add(spacing.plainCopy().append(CreateLang.translateDirect(_knockback, knockback).withStyle(darkGreen)));
    }

    @Override
    @SuppressWarnings("removal")
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SimpleCustomRenderer.create(this, new AirtightCannonItemRenderer()));
    }

    @Override
    public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
        return stack -> AirtightCannonProjectileType.getTypeForItem(GlobalRegistryAccess.getOrThrow(), stack.getItem()).isPresent();
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    @Override
    protected void shoot(@NotNull ServerLevel level, @NotNull LivingEntity shooter, @NotNull InteractionHand hand, @NotNull ItemStack weapon, @NotNull List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit, @Nullable LivingEntity target) {
    }

    @Override
    protected void shootProjectile(@NotNull LivingEntity shooter, @NotNull Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
    }

    @Override
    public boolean onEntitySwing(@NotNull ItemStack stack, @NotNull LivingEntity entity, @NotNull InteractionHand hand) {
        return true;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.INFINITY)) {
            return false;
        }
        if (enchantment.is(Enchantments.LOOTING)) {
            return true;
        }
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }

    @Override
    @Nullable
    public HumanoidModel.ArmPose getArmPose(ItemStack stack, @NotNull AbstractClientPlayer player, InteractionHand hand) {
        if (!player.swinging) {
            return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        }
        return null;
    }

    public record Ammo(ItemStack stack, AirtightCannonProjectileType type) {
    }
}
