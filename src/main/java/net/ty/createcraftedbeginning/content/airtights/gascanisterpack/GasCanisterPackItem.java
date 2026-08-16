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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.GasStackLinkedSet;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.IGasFilter;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterPackItem extends Item implements MenuProvider, IGasFilter {
    public GasCanisterPackItem(Properties properties) {
        super(properties);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(GasHandler.ITEM, (itemStack, context) -> new GasCanisterPackContainerContents(itemStack), CCBItems.GAS_CANISTER_PACK);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return GasCanisterPackUtils.shouldCauseBlockBreakReset(oldStack, newStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.FAIL : use(context.getLevel(), player, context.getHand()).getResult();
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
        if (!(pack.getCapability(GasHandler.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return;
        }

        for (int slot = 0; slot < GasCanisterPackContainerContents.MAX_COUNT; slot++) {
            tooltip.add(CCBLang.translate("gui.gas_canister_pack.number", slot + 1).style(ChatFormatting.GRAY).component());

            GasStack gas = packContents.getGasInTank(slot);
            long capacity = packContents.getTankCapacity(slot);
            boolean isCreative = packContents.getCreatives(slot);
            if (!gas.isEmpty()) {
                tooltip.add(CCBLang.translate("gui.gas_canister.content").add(CCBLang.gasName(gas).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
            }

            if (isCreative) {
                tooltip.add(CCBLang.translate("gui.gas_canister.capacity").add(CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
                continue;
            }

            if (gas.isEmpty()) {
                tooltip.add(CCBLang.translate("gui.gas_canister.capacity").add(GasAmounts.precise(capacity).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
                continue;
            }

            tooltip.add(CCBLang.translate("gui.gas_canister.capacity").add(GasAmounts.precise(gas.getAmount()).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(GasAmounts.precise(capacity).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
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
        if (filterGasStack.isEmpty() || !(filterItem.getCapability(GasHandler.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return false;
        }

        for (int tank = 0; tank < packContents.getTanks(); tank++) {
            GasStack gas = packContents.getGasInTank(tank);
            if (!gas.isEmpty() && GasStack.isSameGasSameComponents(gas, filterGasStack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Predicate<GasStack> compile(ItemStack filterItem) {
        if (!(filterItem.getCapability(GasHandler.ITEM) instanceof GasCanisterPackContainerContents packContents)) {
            return gas -> false;
        }

        Set<GasStack> gases = GasStackLinkedSet.createTypeAndComponentsSet();
        for (int tank = 0; tank < packContents.getTanks(); tank++) {
            GasStack gas = packContents.getGasInTank(tank).copyWithAmount(1);
            if (!gas.isEmpty()) {
                gases.add(gas);
            }
        }

        return gas -> !gas.isEmpty() && gases.contains(gas);
    }
}
