package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.HandheldDrillAttackModeButton;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHandheldDrillItem extends PickaxeItem implements MenuProvider {
    public AirtightHandheldDrillItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack drill, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack drill, LivingEntity target, LivingEntity attacker) {
    }

    @Override
    public boolean isValidRepairItem(ItemStack drill, ItemStack repair) {
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack drill, Player player, Entity entity) {
        return HandheldDrillAttackModeButton.INSTANCE.isActive(player, drill);
    }

    @Override
    public boolean isDamageable(ItemStack drill) {
        return false;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack drill, int remainingUseDuration) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }

        if (!HandheldDrillAttackModeButton.INSTANCE.isActive(player, drill)) {
            player.stopUsingItem();
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            player.stopUsingItem();
            return;
        }

        AirtightHandheldDrillHandler drillHandler = AirtightHandheldDrillHandler.REGISTRY.get(gasContent.getGasType());
        if (drillHandler == null) {
            player.stopUsingItem();
            return;
        }

        int usedTicks = 72000 - remainingUseDuration;
        if (usedTicks <= 4 || usedTicks % 4 != 0) {
            return;
        }

        AirtightHandheldDrillUtils.doDrillAttack(player, level);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        ItemStack drill = context.getItemInHand();
        if (player.isShiftKeyDown() || HandheldDrillAttackModeButton.INSTANCE.isActive(player, drill)) {
            return use(context.getLevel(), player, context.getHand()).getResult();
        }
        return InteractionResult.PASS;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return Tiers.NETHERITE.getSpeed();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack drill = player.getItemInHand(hand);
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(drill);
        }

        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                return InteractionResultHolder.sidedSuccess(drill, true);
            }

            player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, drill));
            player.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.sidedSuccess(drill, false);
        }

        if (!HandheldDrillAttackModeButton.INSTANCE.canApply(drill) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return InteractionResultHolder.fail(drill);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(drill);
    }

    @Override
    public boolean isBarVisible(ItemStack drill) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack drill) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack drill) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    public boolean mineBlock(ItemStack drill, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!(miningEntity instanceof Player player) || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        AirtightHandheldDrillUtils.mineAreaBlocks(drill, serverLevel, pos, player);
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack drill, BlockState state) {
        return true;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack drill) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack drill, LivingEntity entity) {
        return 72000;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack drill, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightHandheldDrillHandler drillHandler = AirtightHandheldDrillHandler.REGISTRY.get(gasContent.getGasType());
        if (drillHandler == null) {
            return;
        }

        drillHandler.appendHoverText(drill, context, tooltip, tooltipFlag);
    }

    @Override
    public boolean isEnchantable(ItemStack drill) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AirtightHandheldDrillMenu(CCBMenuTypes.AIRTIGHT_HANDHELD_DRILL_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
