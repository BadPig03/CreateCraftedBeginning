package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilter;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterItem extends Item implements GasFilter {
    private final Supplier<GasCanisterBlockItem> blockItem;

    public GasCanisterItem(Properties properties, Supplier<GasCanisterBlockItem> blockItem) {
        super(properties);
        this.blockItem = blockItem;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(CanisterCapabilities.ITEM, (canister, ignoredContext) -> new GasCanisterContainerContents(canister), CCBItems.GAS_CANISTER);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return CanisterMiningReset.shouldCauseBlockBreakReset(oldStack, newStack, Set.of(CCBDataComponents.CANISTER_CONTAINER_CONTENTS));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return blockItem.get().useOn(context);
    }

    @Override
    public boolean isBarVisible(ItemStack canister) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack canister) {
        return CanisterContainerClients.getBarWidth(canister);
    }

    @Override
    public int getBarColor(ItemStack canister) {
        return CanisterContainerClients.getBarColor(canister);
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack canister, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        GasStack storedGas = canisterContents.getGasInTank(0);
        long maxAmount = canisterContents.getTankMaxAmount(0);
        if (storedGas.isEmpty()) {
            tooltip.add(CCBLang.translate("gui.gas_canister.max_amount").add(GasUnitFormat.amount(maxAmount).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
        }
        else {
            tooltip.add(CCBLang.translate("gui.gas_canister.content").add(CCBLang.gasName(storedGas).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            tooltip.addAll(storedGas.getGasType().getTooltip(storedGas));
            tooltip.add(CCBLang.translate("gui.gas_canister.max_amount").add(GasUnitFormat.amount(storedGas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasUnitFormat.amount(maxAmount).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
        }
        long pressurePa = canisterContents.getTankPressurePa(0);
        ChatFormatting pressureColor = GasPressureLimits.isOverpressure(pressurePa) ? ChatFormatting.RED : ChatFormatting.GOLD;
        tooltip.add(CCBLang.translate("gui.gas_container.pressure").add(CCBLang.text(GasPressure.formatAtm(pressurePa)).style(pressureColor)).style(ChatFormatting.GRAY).component());
    }

    @Override
    public boolean isEnchantable(ItemStack canister) {
        return true;
    }

    @Override
    public boolean test(ItemStack filterItem, GasStack filterGasStack) {
        if (filterGasStack.isEmpty() || !(filterItem.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents filterContents)) {
            return false;
        }

        GasStack filterGas = filterContents.getGasInTank(0);
        return !filterGas.isEmpty() && GasStack.isSameGasSameComponents(filterGas, filterGasStack);
    }

    @Override
    public Predicate<GasStack> compile(ItemStack filterItem) {
        if (!(filterItem.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents filterContents)) {
            return ignoredGas -> false;
        }

        GasStack filterGas = filterContents.getGasInTank(0).copyWithAmount(1);
        if (filterGas.isEmpty()) {
            return ignoredGas -> false;
        }

        return candidateGas -> !candidateGas.isEmpty() && GasStack.isSameGasSameComponents(filterGas, candidateGas);
    }

    public static class GasCanisterBlockItem extends BlockItem {
        private final Supplier<Item> actualItem;

        public GasCanisterBlockItem(Block block, Supplier<Item> actualItem, Properties properties) {
            super(block, properties.fireResistant());
            this.actualItem = actualItem;
        }

        @Override
        public String getDescriptionId() {
            return getOrCreateDescriptionId();
        }

        Item getActualItem() {
            return actualItem.get();
        }
    }
}
