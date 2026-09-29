package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.CanisterMiningReset;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilter;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.recipe.gas.ingredient.GasStackLinkedSet;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterPackItem extends Item implements MenuProvider, GasFilter {
    public GasCanisterPackItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return CanisterMiningReset.shouldCauseBlockBreakReset(oldStack, newStack, Set.of(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, CCBDataComponents.GAS_CANISTER_PACK_CONTENTS));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        return use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pack = player.getItemInHand(hand);
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(pack);
        }

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(pack, true);
        }

        player.openMenu(this, buffer -> ItemStack.STREAM_CODEC.encode(buffer, pack));
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(pack, false);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack pack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!(pack.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        for (int tankIndex = 0; tankIndex < GasCanisterPackContainerContents.MAX_COUNT; tankIndex++) {
            GasStack gasContent = packContents.getGasInTank(tankIndex);
            long maxAmount = packContents.getTankMaxAmount(tankIndex);
            boolean isCreative = packContents.isCreative(tankIndex);
            tooltip.add(CCBLang.translate("gui.gas_canister_pack.number", tankIndex + 1).style(ChatFormatting.GRAY).component());
            if (!gasContent.isEmpty()) {
                tooltip.add(CCBLang.translate("gui.gas_canister.content").add(CCBLang.gasName(gasContent).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            }

            if (isCreative) {
                tooltip.add(CCBLang.translate("gui.gas_canister.max_amount").add(CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            }
            else if (gasContent.isEmpty()) {
                tooltip.add(CCBLang.translate("gui.gas_canister.max_amount").add(GasUnitFormat.amount(maxAmount).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            }
            else {
                tooltip.add(CCBLang.translate("gui.gas_canister.max_amount").add(GasUnitFormat.amount(gasContent.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasUnitFormat.amount(maxAmount).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
            }
            if (packContents.getCanister(tankIndex).isEmpty()) {
                continue;
            }

            long pressurePa = packContents.getTankPressurePa(tankIndex);
            ChatFormatting pressureColor = GasPressureLimits.isOverpressure(pressurePa) ? ChatFormatting.RED : ChatFormatting.GOLD;
            tooltip.add(CCBLang.translate("gui.gas_container.pressure").add(CCBLang.text(GasPressure.formatAtm(pressurePa)).style(pressureColor)).style(ChatFormatting.GRAY).component());
        }
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new GasCanisterPackMenu(CCBMenuTypes.GAS_CANISTER_PACK_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }

    @Override
    public boolean test(ItemStack filterItem, GasStack filterGasStack) {
        if (filterGasStack.isEmpty() || !(filterItem.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < packContents.getTanks(); tankIndex++) {
            GasStack gasContent = packContents.getGasInTank(tankIndex);
            if (gasContent.isEmpty() || !GasStack.isSameGasSameComponents(gasContent, filterGasStack)) {
                continue;
            }

            return true;
        }
        return false;
    }

    @Override
    public Predicate<GasStack> compile(ItemStack filterItem) {
        if (!(filterItem.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return gas -> false;
        }

        Set<GasStack> acceptedGases = GasStackLinkedSet.createTypeAndComponentsSet();
        for (int tankIndex = 0; tankIndex < packContents.getTanks(); tankIndex++) {
            GasStack gasType = packContents.getGasInTank(tankIndex).copyWithAmount(1);
            if (gasType.isEmpty()) {
                continue;
            }

            acceptedGases.add(gasType);
        }

        return candidateGas -> !candidateGas.isEmpty() && acceptedGases.contains(candidateGas);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(CanisterCapabilities.ITEM, (itemStack, context) -> new GasCanisterPackContainerContents(itemStack), CCBItems.GAS_CANISTER_PACK);
    }
}
