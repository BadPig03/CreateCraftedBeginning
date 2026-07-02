package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.ItemCopyingRecipe.SupportsItemCopying;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.CommonComponents;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFilterItem extends Item implements MenuProvider, SupportsItemCopying, IGasFilter {
    public GasFilterItem(Properties properties) {
        super(properties);
    }

    public static boolean isBlacklist(ItemStack filter) {
        return filter.getOrDefault(AllDataComponents.FILTER_ITEMS_BLACKLIST, false);
    }

    public static ItemStackHandler getGasFilterItemHandler(ItemStack filter) {
        ItemStackHandler inv = new ItemStackHandler(18);
        ItemHelper.fillItemStackHandler(filter.getOrDefault(AllDataComponents.FILTER_ITEMS, ItemContainerContents.EMPTY), inv);
        return inv;
    }

    public static List<Gas> getExistingGasTypes(ItemStackHandler filterInventory) {
        List<Gas> existingGasTypes = new ArrayList<>();
        for (int i = 0; i < filterInventory.getSlots(); i++) {
            ItemStack stack = filterInventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            Gas gasType = stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_TYPE, GasStack.EMPTY).getGasType();
            if (gasType.isEmpty()) {
                continue;
            }

            existingGasTypes.add(gasType);
        }
        return existingGasTypes;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        return use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack filter = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(filter);
        }

        if (!level.isClientSide) {
            player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, filter));
        }
        return InteractionResultHolder.success(filter);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack filter, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        if (AllKeys.shiftDown()) {
            return;
        }

        List<Gas> existingGasTypes = getExistingGasTypes(getGasFilterItemHandler(filter));
        if (existingGasTypes.isEmpty()) {
            return;
        }

        tooltips.add(CommonComponents.EMPTY);
        if (isBlacklist(filter)) {
            tooltips.add(CCBLang.translateDirect("gui.gas_filter.blacklist").withStyle(ChatFormatting.GOLD));
        }
        else {
            tooltips.add(CCBLang.translateDirect("gui.gas_filter.whitelist").withStyle(ChatFormatting.GOLD));
        }

        int count = 0;
        for (Gas gasType : existingGasTypes) {
            if (count > 3) {
                tooltips.add(CCBLang.text("- ...").style(ChatFormatting.DARK_GRAY).component());
                return;
            }

            tooltips.add(CCBLang.text("- ").add(Component.translatable(gasType.getTranslationKey())).style(ChatFormatting.GRAY).component());
            count++;
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return GasFilterMenu.create(id, inv, player.getMainHandItem());
    }

    @Override
    public DataComponentType<?> getComponentType() {
        return AllDataComponents.FILTER_ITEMS;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public boolean test(ItemStack filterItem, GasStack filterGasStack) {
        if (!filterItem.is(CCBItems.GAS_FILTER)) {
            return false;
        }

        List<Gas> existingGasTypes = getExistingGasTypes(getGasFilterItemHandler(filterItem));
        if (isBlacklist(filterItem)) {
            return existingGasTypes.stream().noneMatch(filterGasStack::is);
        }
        else {
            return existingGasTypes.stream().anyMatch(filterGasStack::is);
        }
    }
}
