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
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasCanisterPackItem extends Item implements MenuProvider {
    public GasCanisterPackItem(Properties properties) {
        super(properties);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerItem(GasHandler.ITEM, (itemStack, context) -> new GasCanisterPackContainerContents(itemStack), CCBItems.GAS_CANISTER_PACK);
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
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack pack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(pack.getCapability(GasHandler.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        for (int slot = 0; slot < 4; slot++) {
            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister_pack.number", slot + 1).style(ChatFormatting.GRAY).component());
            GasStack gasContent = packContents.getGasInTank(slot);
            long capacity = packContents.getTankCapacity(slot);
            boolean creative = packContents.getCreatives(slot);
            if (gasContent.isEmpty()) {
                if (creative) {
                    tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.translate("gui.goggles.gas_container.infinity").style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
                }
                else {
                    tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
                }
                continue;
            }

            tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.content").add(CCBLang.gasName(gasContent).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            if (creative) {
                tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.translate("gui.goggles.gas_container.infinity").style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            }
            else {
                tooltip.add(CCBLang.translate("gui.tooltips.gas_canister.capacity").add(CCBLang.number(gasContent.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
            }
        }
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
