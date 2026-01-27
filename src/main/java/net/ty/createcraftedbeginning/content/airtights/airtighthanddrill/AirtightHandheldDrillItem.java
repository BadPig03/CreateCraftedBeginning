package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AirtightHandheldDrillItem extends PickaxeItem implements MenuProvider {
    public AirtightHandheldDrillItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack drill, @NotNull Player player, @NotNull Entity entity) {
        return CanisterContainerSuppliers.isAnyContainerAvailable(player) || AirtightHandheldDrillUtils.isDrillAttackEnabled(drill);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack drill, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(Enchantments.EFFICIENCY);
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return AirtightHandheldDrillUtils.shouldCauseBlockBreakReset(oldStack, newStack);
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack drill) {
        return false;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack drill, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(@NotNull ItemStack drill, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack drill, @NotNull ItemStack repair) {
        return false;
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        return player == null || !player.isShiftKeyDown() ? InteractionResult.FAIL : use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        return Tiers.NETHERITE.getSpeed();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack drill = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(drill);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(drill, true);
        }

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, drill));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(drill, false);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack drill) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(@NotNull ItemStack drill) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(@NotNull ItemStack drill) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack drill, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity miningEntity) {
        if (!(miningEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        AirtightHandheldDrillUtils.mineAreaBlocks(drill, serverLevel, pos, player);
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack drill, @NotNull BlockState state) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack drill, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        AirtightHandheldDrillUtils.appendHoverText(drill, context, tooltip, tooltipFlag);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new AirtightHandheldDrillMenu(CCBMenuTypes.AIRTIGHT_HANDHELD_DRILL_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
