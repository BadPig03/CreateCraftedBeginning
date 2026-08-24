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
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
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

    protected ItemStack filterItem = ItemStack.EMPTY;
    protected Predicate<GasStack> compiledFilter = GasFilterUtils.compile(ItemStack.EMPTY);

    private static long saturatedAdd(long currentAmount, long addedAmount) {
        if (addedAmount <= 0) {
            return currentAmount;
        }
        return currentAmount > Long.MAX_VALUE - addedAmount ? Long.MAX_VALUE : currentAmount + addedAmount;
    }

    private static boolean testLong(Ops operator, long currentAmount, long targetAmount) {
        return switch (operator) {
            case GREATER -> currentAmount > targetAmount;
            case EQUAL -> currentAmount == targetAmount;
            case LESS -> currentAmount < targetAmount;
        };
    }

    @Override
    protected boolean test(Level level, Train train, CompoundTag context) {
        Ops operator = getOperator();
        long targetAmount = Math.max(0, (long) getThreshold() * GasAmounts.MILLIBUCKETS_PER_BUCKET);
        long totalAmount = 0;
        for (Carriage carriage : train.carriages) {
            if (!(carriage.storage instanceof IMountedStorageManagerWithGas gasStorageManager)) {
                continue;
            }

            MountedGasStorageWrapper gasStorage = gasStorageManager.ccb$getGases();
            for (int tankIndex = 0; tankIndex < gasStorage.getTanks(); tankIndex++) {
                GasStack storedGas = gasStorage.getGasInTank(tankIndex);
                if (storedGas.isEmpty() || !compiledFilter.test(storedGas)) {
                    continue;
                }

                totalAmount = saturatedAdd(totalAmount, storedGas.getAmount());
            }
        }

        requestStatusToUpdate(GasAmounts.toWholeBucketsClamped(totalAmount), context);
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

        int thresholdOffset = switch (getOperator()) {
            case LESS -> -1;
            case GREATER -> 1;
            case EQUAL -> 0;
        };
        return CCBLang.translateDirect("schedule.condition.threshold.status", lastDisplaySnapshot, Math.max(0, getThreshold() + thresholdOffset), CCBLang.translateDirect("gui.threshold.buckets"));
    }

    @Override
    public ResourceLocation getId() {
        return CCBAPI.asResource("gas_threshold");
    }

    @Override
    public List<Component> getTitleAs(String type) {
        List<Component> titleLines = new ArrayList<>();
        Component operatorName = CCBLang.translateDirect("schedule.condition.threshold." + Lang.asId(getOperator().name()));
        titleLines.add(CCBLang.translateDirect("schedule.condition.threshold.train_holds", operatorName));
        Component filterDescription;
        if (filterItem.isEmpty()) {
            filterDescription = CCBLang.translateDirect("schedule.condition.threshold.anything");
        }
        else if (GasFilterUtils.isFilter(filterItem)) {
            filterDescription = CCBLang.translateDirect("schedule.condition.threshold.matching_gas_content");
        }
        else {
            filterDescription = CCBLang.translateDirect("schedule.condition.threshold.invalid_gas_filter");
        }
        titleLines.add(CCBLang.translateDirect("schedule.condition.threshold.x_units_of_item", getThreshold(), CCBLang.translateDirect("gui.threshold.buckets"), filterDescription).withStyle(ChatFormatting.DARK_AQUA));
        return titleLines;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        updateFilter(stack);
    }

    @Override
    public ItemStack getItem(int slot) {
        return filterItem.copy();
    }

    protected void updateFilter(ItemStack filterStack) {
        filterItem = GasFilterUtils.normalizeStack(filterStack);
        compiledFilter = GasFilterUtils.compile(filterItem);
    }
}
