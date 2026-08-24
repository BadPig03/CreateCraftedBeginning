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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerClients;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.IGasFilter;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterItem extends Item implements IGasFilter {
    private final Supplier<GasCanisterBlockItem> blockItem;

    public GasCanisterItem(Properties properties, Supplier<GasCanisterBlockItem> blockItem) {
        super(properties);
        this.blockItem = blockItem;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(GasHandler.ITEM, (canister, ignoredContext) -> new GasCanisterContainerContents(canister), CCBItems.GAS_CANISTER);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return GasCanisterUtils.shouldCauseBlockBreakReset(oldStack, newStack);
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
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        GasStack storedGas = canisterContents.getGasInTank(0);
        long capacity = canisterContents.getTankCapacity(0);
        if (storedGas.isEmpty()) {
            tooltip.add(CCBLang.translate("gui.gas_canister.capacity").add(GasAmounts.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            return;
        }

        tooltip.add(CCBLang.translate("gui.gas_canister.content").add(CCBLang.gasName(storedGas).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
        tooltip.add(CCBLang.translate("gui.gas_canister.capacity").add(GasAmounts.precise(storedGas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(capacity).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
    }

    @Override
    public boolean isEnchantable(ItemStack canister) {
        return true;
    }

    public Block getBlock() {
        return blockItem.get().getBlock();
    }

    @Override
    public boolean test(ItemStack filterItem, GasStack filterGasStack) {
        if (filterGasStack.isEmpty() || !(filterItem.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents filterContents)) {
            return false;
        }

        GasStack filterGas = filterContents.getGasInTank(0);
        return !filterGas.isEmpty() && GasStack.isSameGasSameComponents(filterGas, filterGasStack);
    }

    @Override
    public Predicate<GasStack> compile(ItemStack filterItem) {
        if (!(filterItem.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents filterContents)) {
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

        public Item getActualItem() {
            return actualItem.get();
        }
    }
}
