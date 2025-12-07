package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class GasCanisterPackItem extends Item implements MenuProvider {
    public GasCanisterPackItem(Properties properties) {
        super(properties);
    }

    public void registerModelOverrides() {
        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> GasCanisterPackOverrides.registerModelOverridesClient(this));
    }

    @Override
    public boolean shouldCauseReequipAnimation(@NotNull ItemStack oldStack, @NotNull ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {
        return GasCanisterPackUtils.shouldCauseBlockBreakReset(oldStack, newStack);
    }

    @Override
    public void onCraftedPostProcess(@NotNull ItemStack pack, @NotNull Level level) {
        if (level.isClientSide) {
            return;
        }

        GasCanisterPackUtils.resetCanisterPackUUID(pack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack pack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide) {
            return;
        }

        GasCanisterPackUtils.resetCanisterPackUUID(pack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack pack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        UUID uuid = GasCanisterPackUtils.getCanisterPackUUID(pack);
        GasCanisterPackContents contents = CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.getContents(uuid);
        for (int slot = 0; slot < 4; slot++) {
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister_pack.number", slot + 1).style(ChatFormatting.GRAY).component());
            ItemStack canister = contents.getStackInSlot(slot).copy();
            if (canister.isEmpty()) {
                tooltip.add(CCBLang.translate("gui.goggles.gas_container.empty").style(ChatFormatting.DARK_GRAY).component());
                continue;
            }

            LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
            GasStack gasContent = GasCanisterQueryUtils.getCanisterContent(canister);
            long capacity = GasCanisterQueryUtils.getCanisterCapacity(canister, gasContent.getGas());
            if (gasContent.isEmpty()) {
                tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
                continue;
            }

            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.content").add(CCBLang.gasName(gasContent).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.number(gasContent.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
        }
    }

    @NotNull
    @Override
    public InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.FAIL : use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack drill = player.getItemInHand(hand);
        if (hand == InteractionHand.OFF_HAND) {
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
    public @NotNull Component getDisplayName() {
        return getDescription();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new GasCanisterPackMenu(CCBMenuTypes.GAS_CANISTER_PACK_MENU.get(), containerId, playerInventory, player.getMainHandItem());
    }
}
