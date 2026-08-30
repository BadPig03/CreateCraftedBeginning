package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.IGasFilter;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeGasCanisterItem extends Item implements IGasFilter {
    private final Supplier<CreativeGasCanisterBlockItem> blockItem;

    public CreativeGasCanisterItem(Properties properties, Supplier<CreativeGasCanisterBlockItem> blockItem) {
        super(properties);
        this.blockItem = blockItem;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(GasHandler.ITEM, (canister, ignoredContext) -> new CreativeGasCanisterContainerContents(canister), CCBItems.CREATIVE_GAS_CANISTER);
    }

    private static boolean hasGas(ItemStack canister) {
        return canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents && !canisterContents.isEmpty();
    }

    @Override
    public boolean supportsEnchantment(ItemStack canister, Holder<Enchantment> enchantment) {
        return false;
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
        if (!hasGas(canister)) {
            return 0;
        }
        return 13;
    }

    @Override
    public int getBarColor(ItemStack canister) {
        if (!hasGas(canister)) {
            return 0;
        }
        return GasCanisterUtils.COLOR_WHITE;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack canister, ItemStack sourceCanister, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!(sourceCanister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents sourceContents)) {
            return false;
        }

        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents targetContents)) {
            return false;
        }

        if (action == ClickAction.PRIMARY) {
            return false;
        }

        targetContents.setGasInTank(0, sourceContents.getGasInTank(0));
        return true;
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack canister, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return;
        }

        GasStack storedGas = canisterContents.getGasInTank(0);
        if (storedGas.isEmpty()) {
            tooltip.add(CCBLang.translate("gui.gas_canister.content").add(CCBLang.translate("gui.creative_gas_canister.empty")).style(ChatFormatting.GRAY).component());
            return;
        }

        tooltip.add(CCBLang.translate("gui.gas_canister.content").add(CCBLang.gasName(storedGas).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
    }

    @Override
    public boolean isEnchantable(ItemStack canister) {
        return false;
    }

    @Override
    public boolean test(ItemStack filterItem, GasStack filterGasStack) {
        if (filterGasStack.isEmpty() || !(filterItem.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents filterContents)) {
            return false;
        }

        GasStack filterGas = filterContents.getGasInTank(0);
        return !filterGas.isEmpty() && GasStack.isSameGasSameComponents(filterGas, filterGasStack);
    }

    @Override
    public Predicate<GasStack> compile(ItemStack filterItem) {
        if (!(filterItem.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents filterContents)) {
            return ignoredGas -> false;
        }

        GasStack filterGas = filterContents.getGasInTank(0).copyWithAmount(1);
        if (filterGas.isEmpty()) {
            return ignoredGas -> false;
        }
        return candidateGas -> !candidateGas.isEmpty() && GasStack.isSameGasSameComponents(filterGas, candidateGas);
    }

    public static class CreativeGasCanisterBlockItem extends BlockItem {
        private final Supplier<Item> actualItem;

        public CreativeGasCanisterBlockItem(Block block, Supplier<Item> actualItem, Properties properties) {
            super(block, properties.fireResistant().rarity(Rarity.EPIC));
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
