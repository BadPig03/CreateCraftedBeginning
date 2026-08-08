package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.condition.CargoThresholdCondition;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.createmod.catnip.lang.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasThresholdCondition extends CargoThresholdCondition {
    private static final String COMPOUND_KEY_GAS_FILTER = "GasFilter";

    private ItemStack filterItem = ItemStack.EMPTY;
    private Predicate<GasStack> compiledFilter = GasFilterUtils.compile(ItemStack.EMPTY);

    private static long saturatedAdd(long current, long addition) {
        if (addition <= 0) {
            return current;
        }
        return current > Long.MAX_VALUE - addition ? Long.MAX_VALUE : current + addition;
    }

    private static boolean testLong(Ops operator, long current, long target) {
        return switch (operator) {
            case GREATER -> current > target;
            case EQUAL -> current == target;
            case LESS -> current < target;
        };
    }

    @Override
    protected boolean test(Level level, Train train, CompoundTag context) {
        Ops operator = getOperator();
        long targetAmount = Math.max(0, (long) getThreshold() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET);
        long totalAmount = 0;
        for (Carriage carriage : train.carriages) {
            if (!(carriage.storage instanceof IMountedStorageManagerWithGas withGas)) {
                continue;
            }

            MountedGasStorageWrapper gases = withGas.ccb$getGases();
            for (int i = 0; i < gases.getTanks(); i++) {
                GasStack gas = gases.getGasInTank(i);
                if (gas.isEmpty() || !compiledFilter.test(gas)) {
                    continue;
                }

                totalAmount = saturatedAdd(totalAmount, gas.getAmount());
            }
        }

        requestStatusToUpdate(GasAmountUtils.toWholeBucketsClamped(totalAmount), context);
        return testLong(operator, totalAmount, targetAmount);
    }

    @Override
    protected Component getUnit() {
        return Component.literal("b");
    }

    @Override
    protected ItemStack getIcon() {
        return filterItem;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        super.initConfigurationWidgets(builder);
        builder.addSelectionScrollInput(71, 50, (input, ignoredLabel) -> input.forOptions(List.of(CCBLang.translateDirect("gui.threshold.buckets"))).titled(null), "Measure");
    }

    @Override
    protected void writeAdditional(Provider provider, CompoundTag compoundTag) {
        super.writeAdditional(provider, compoundTag);
        compoundTag.put(COMPOUND_KEY_GAS_FILTER, filterItem.saveOptional(provider));
    }

    @Override
    protected void readAdditional(Provider provider, CompoundTag compoundTag) {
        super.readAdditional(provider, compoundTag);
        ItemStack savedFilter = ItemStack.EMPTY;
        if (compoundTag.contains(COMPOUND_KEY_GAS_FILTER)) {
            savedFilter = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS_FILTER));
        }
        updateFilter(savedFilter);
    }

    @Override
    public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag tag) {
        int lastDisplaySnapshot = getLastDisplaySnapshot(tag);
        if (lastDisplaySnapshot == -1) {
            return Component.empty();
        }

        int offset = switch (getOperator()) {
            case LESS -> -1;
            case GREATER -> 1;
            case EQUAL -> 0;
        };
        return CCBLang.translateDirect("schedule.condition.threshold.status", lastDisplaySnapshot, Math.max(0, getThreshold() + offset), CCBLang.translateDirect("gui.threshold.buckets"));
    }

    @Override
    public ResourceLocation getId() {
        return CCBAPI.asResource("gas_threshold");
    }

    @Override
    public List<Component> getTitleAs(String type) {
        List<Component> lines = new ArrayList<>();
        Component operatorName = CCBLang.translateDirect("schedule.condition.threshold." + Lang.asId(getOperator().name()));
        lines.add(CCBLang.translateDirect("schedule.condition.threshold.train_holds", operatorName));
        Component content;
        if (filterItem.isEmpty()) {
            content = CCBLang.translateDirect("schedule.condition.threshold.anything");
        }
        else if (GasFilterUtils.isFilter(filterItem)) {
            content = CCBLang.translateDirect("schedule.condition.threshold.matching_gas_content");
        }
        else {
            content = GasStack.EMPTY.getHoverName();
        }
        lines.add(CCBLang.translateDirect("schedule.condition.threshold.x_units_of_item", getThreshold(), CCBLang.translateDirect("gui.threshold.buckets"), content).withStyle(ChatFormatting.DARK_AQUA));
        return lines;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        updateFilter(stack);
    }

    @Override
    public ItemStack getItem(int slot) {
        return filterItem.copy();
    }

    private void updateFilter(ItemStack stack) {
        filterItem = stack.isEmpty() || GasFilterUtils.isFilter(stack) ? GasFilterUtils.normalizeStack(stack) : ItemStack.EMPTY;
        compiledFilter = GasFilterUtils.compile(filterItem);
    }
}
