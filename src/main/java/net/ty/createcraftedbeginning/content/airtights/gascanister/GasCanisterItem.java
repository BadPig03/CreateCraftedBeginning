package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.IGasFilter;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterItem extends Item implements IGasFilter {
    private final Supplier<GasCanisterBlockItem> blockItem;

    public GasCanisterItem(Properties properties, Supplier<GasCanisterBlockItem> placeable) {
        super(properties);
        blockItem = placeable;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(GasHandler.ITEM, (itemStack, context) -> new GasCanisterContainerContents(itemStack), CCBItems.GAS_CANISTER);
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
    public InteractionResult useOn(UseOnContext ctx) {
        return blockItem.get().useOn(ctx);
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
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        long capacity = canisterContents.getTankCapacity(0);
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        if (gasContent.isEmpty()) {
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
        }
        else {
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.content").add(CCBLang.gasName(gasContent).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.number(Mth.clamp(gasContent.getAmount(), 0, capacity)).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
        }
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
        if (!(filterItem.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return false;
        }

        GasStack gasContent = canisterContents.getGasInTank(0);
        return !gasContent.isEmpty() && GasStack.isSameGasSameComponents(gasContent, filterGasStack);
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
