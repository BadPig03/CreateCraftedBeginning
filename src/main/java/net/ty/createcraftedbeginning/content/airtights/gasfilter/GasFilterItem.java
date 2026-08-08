package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.simibubi.create.AllKeys;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterUtils.GasFilterData;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFilterItem extends Item implements MenuProvider, SupportsItemCopying, IGasFilter {
    public GasFilterItem(Properties properties) {
        super(properties);
    }

    public static GasFilterData getFilterData(ItemStack filter) {
        return filter.getOrDefault(CCBDataComponents.GAS_FILTER_DATA, GasFilterData.EMPTY);
    }

    public static void setFilterData(ItemStack filter, GasFilterData data) {
        if (data.isDefault()) {
            filter.remove(CCBDataComponents.GAS_FILTER_DATA);
            return;
        }

        filter.set(CCBDataComponents.GAS_FILTER_DATA, data);
    }

    public static ItemStackHandler createFilterInventory(GasFilterData data) {
        ItemStackHandler inventory = new ItemStackHandler(GasFilterData.MAX_ENTRIES);
        List<GasStack> gases = data.gases();
        for (int i = 0; i < gases.size(); i++) {
            inventory.setStackInSlot(i, GasVirtualUtils.createVirtualItem(gases.get(i)));
        }
        return inventory;
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

        if (level.isClientSide) {
            return InteractionResultHolder.success(filter);
        }

        player.openMenu(this, buf -> ItemStack.STREAM_CODEC.encode(buf, filter));
        return InteractionResultHolder.success(filter);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack filter, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        if (AllKeys.shiftDown()) {
            return;
        }

        GasFilterData data = getFilterData(filter);
        List<GasStack> gases = data.gases();
        if (gases.isEmpty()) {
            return;
        }

        tooltips.add(CommonComponents.EMPTY);
        if (data.blacklist()) {
            tooltips.add(CCBLang.translateDirect("gui.gas_filter.blacklist").withStyle(ChatFormatting.GOLD));
        }
        else {
            tooltips.add(CCBLang.translateDirect("gui.gas_filter.whitelist").withStyle(ChatFormatting.GOLD));
        }

        int count = 0;
        for (GasStack gas : gases) {
            if (count > 3) {
                tooltips.add(CCBLang.text("- ...").style(ChatFormatting.DARK_GRAY).component());
                return;
            }

            tooltips.add(CCBLang.text("- ").add(Component.translatable(gas.getGasType().getTranslationKey())).style(ChatFormatting.GRAY).component());
            count++;
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return GasFilterMenu.create(id, inv, player.getMainHandItem());
    }

    @Override
    public DataComponentType<?> getComponentType() {
        return CCBDataComponents.GAS_FILTER_DATA;
    }

    @Override
    public Component getDisplayName() {
        return getDescription();
    }

    @Override
    public boolean test(ItemStack filterItem, GasStack filterGasStack) {
        return filterItem.is(CCBItems.GAS_FILTER) && getFilterData(filterItem).test(filterGasStack);
    }

    @Override
    public Predicate<GasStack> compile(ItemStack filterItem) {
        if (!filterItem.is(CCBItems.GAS_FILTER)) {
            return gas -> false;
        }
        return getFilterData(filterItem).compile();
    }
}
