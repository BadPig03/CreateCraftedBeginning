package net.ty.createcraftedbeginning.content.airtights.creativegascanister;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class CreativeGasCanisterItem extends Item implements IGasFilter {
    private final Supplier<CreativeGasCanisterBlockItem> blockItem;

    public CreativeGasCanisterItem(Properties properties, Supplier<CreativeGasCanisterBlockItem> placeable) {
        super(properties);
        blockItem = placeable;
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerItem(GasHandler.ITEM, (itemStack, context) -> new CreativeGasCanisterContainerContents(itemStack), CCBItems.CREATIVE_GAS_CANISTER);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack canister, @NotNull Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return GasCanisterUtils.shouldCauseBlockBreakReset(oldStack, newStack);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        return blockItem.get().useOn(ctx);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack canister) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents) || canisterContents.isEmpty()) {
            return 0;
        }

        return 13;
    }

    @Override
    public int getBarColor(@NotNull ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents) || canisterContents.isEmpty()) {
            return 0;
        }

        return GasCanisterUtils.COLOR_WHITE;
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack canister, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess access) {
        if (!(other.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents) || !(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents creativeCanisterContents)) {
            return false;
        }

        creativeCanisterContents.setGasInTank(0, canisterContents.getGasInTank(0));
        return true;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack canister, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(canister.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        if (gasContent.isEmpty()) {
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.content").add(CCBLang.translate("gui.tooltips.creative_gas_canister.empty")).style(ChatFormatting.GRAY).component());
        }
        else {
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.content").add(CCBLang.gasName(gasContent).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
        }
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack canister) {
        return false;
    }

    public Block getBlock() {
        return blockItem.get().getBlock();
    }

    @Override
    public boolean test(@NotNull ItemStack filterItem, GasStack filterGasStack) {
        if (!(filterItem.getCapability(GasHandler.ITEM) instanceof CreativeGasCanisterContainerContents canisterContents)) {
            return false;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        return !gasContent.isEmpty() && GasStack.isSameGasSameComponents(gasContent, filterGasStack);
    }

    public static class CreativeGasCanisterBlockItem extends BlockItem {
        private final Supplier<Item> actualItem;

        public CreativeGasCanisterBlockItem(Block block, @NotNull Supplier<Item> actualItem, @NotNull Properties properties) {
            super(block, properties.fireResistant().rarity(Rarity.EPIC));
            this.actualItem = actualItem;
        }

        @Override
        public @NotNull String getDescriptionId() {
            return getOrCreateDescriptionId();
        }

        public Item getActualItem() {
            return actualItem.get();
        }
    }
}
