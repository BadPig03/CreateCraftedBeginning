package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public class GasCanisterItem extends Item {
    private final Supplier<GasCanisterBlockItem> blockItem;

    public GasCanisterItem(Properties properties, Supplier<GasCanisterBlockItem> placeable) {
        super(properties);
        blockItem = placeable;
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack canister, @NotNull Holder<Enchantment> enchantment) {
        return enchantment.is(AllEnchantments.CAPACITY) || enchantment.is(CCBEnchantments.ECONOMIZE);
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
        return GasCanisterUtils.getBarWidth(canister);
    }

    @Override
    public int getBarColor(@NotNull ItemStack canister) {
        return GasCanisterUtils.getBarColor(canister);
    }

    @Override
    public @NotNull String getDescriptionId() {
        return getOrCreateDescriptionId();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack canister, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        GasStack gasContent = GasCanisterQueryUtils.getCanisterContent(canister);
        long capacity = GasCanisterQueryUtils.getCanisterCapacity(canister, gasContent.getGas());
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
    public boolean isEnchantable(@NotNull ItemStack canister) {
        return true;
    }

    public Block getBlock() {
        return blockItem.get().getBlock();
    }

    public static class GasCanisterBlockItem extends BlockItem {
        private final Supplier<Item> actualItem;

        public GasCanisterBlockItem(Block block, @NotNull Supplier<Item> actualItem, @NotNull Properties properties) {
            super(block, properties.fireResistant());
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
