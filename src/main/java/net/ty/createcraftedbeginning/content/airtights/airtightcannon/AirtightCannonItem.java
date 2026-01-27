package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.weatherflares.WeatherFlareSupplierUtils;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class AirtightCannonItem extends ProjectileWeaponItem implements CustomArmPoseItem {
    private static final int MAX_CHARGE_TIME = 72000;

    public AirtightCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player) {
        return false;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        return CanisterContainerSuppliers.isAnyContainerAvailable(player) ? use(context.getLevel(), player, context.getHand()).getResult() : InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack cannon = player.getItemInHand(hand);
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return InteractionResultHolder.fail(cannon);
        }
        if (ShootableGadgetItemMethods.shouldSwap(player, cannon, hand, stack -> stack.is(CCBItems.AIRTIGHT_CANNON))) {
            return InteractionResultHolder.fail(cannon);
        }

        player.startUsingItem(hand);
        if (level.isClientSide) {
            CreateCraftedBeginningClient.AIRTIGHT_CANNON_RENDER_HANDLER.dontAnimateItem(hand);
        }

        return InteractionResultHolder.success(cannon);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack cannon) {
        return false;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack cannon) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack cannon) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack cannon) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack cannon, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity miningEntity) {
        return false;
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack cannon, @NotNull BlockState state) {
        return 0;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack cannon) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack cannon, @NotNull LivingEntity entity) {
        return MAX_CHARGE_TIME;
    }

    @Override
    public void releaseUsing(@NotNull ItemStack cannon, @NotNull Level level, @NotNull LivingEntity entity, int timeCharged) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        float chargedRatio = AirtightCannonUtils.getChargedRatio(cannon, player, MAX_CHARGE_TIME - timeCharged);
        if (chargedRatio == -1) {
            return;
        }

        ItemStack flareItem = WeatherFlareSupplierUtils.getFirstFlare(player);
        if (!flareItem.isEmpty()) {
            AirtightCannonUtils.fireFlares(level, player, flareItem, chargedRatio);
            return;
        }

        AirtightCannonUtils.spawnWindCharges(level, player, chargedRatio);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightCannonUtils.appendHoverText(cannon, context, tooltip, tooltipFlag);
    }

    @Override
    public boolean onEntitySwing(@NotNull ItemStack cannon, @NotNull LivingEntity entity, @NotNull InteractionHand hand) {
        return true;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack cannon, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.MULTISHOT) || enchantment.is(Enchantments.QUICK_CHARGE) || enchantment.is(Enchantments.PUNCH);
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }

    @Override
    public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
        return ItemStack::isEmpty;
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
    @Nullable
    public ArmPose getArmPose(ItemStack cannon, @NotNull AbstractClientPlayer player, InteractionHand hand) {
        return player.swinging ? null : ArmPose.CROSSBOW_HOLD;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack cannon, @NotNull ItemStack repair) {
        return false;
    }
}
