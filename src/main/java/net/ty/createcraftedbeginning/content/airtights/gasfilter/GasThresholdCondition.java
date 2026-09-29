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
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageAccess;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageWrapper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasThresholdCondition extends CargoThresholdCondition {
    private static final String COMPOUND_KEY_GAS_FILTER = "GasFilter";

    private ItemStack filterItem = ItemStack.EMPTY;

    @Override
    protected boolean test(Level level, Train train, CompoundTag context) {
        Ops operator = getOperator();
        long targetAmount = Math.max(0, getThreshold() * GasUnits.GU_PER_KGU);
        long totalAmount = 0;
        for (Carriage carriage : train.carriages) {
            if (!(carriage.storage instanceof MountedGasStorageAccess gasStorageManager)) {
                continue;
            }

            MountedGasStorageWrapper gasStorage = gasStorageManager.ccb$getGasStorage();
            for (int tankIndex = 0; tankIndex < gasStorage.getTanks(); tankIndex++) {
                GasStack storedGas = gasStorage.getGasInTank(tankIndex);
                if (!GasFilters.matches(filterItem, storedGas)) {
                    continue;
                }

                totalAmount = BoundedMath.saturatedAdd(totalAmount, storedGas.getAmount());
            }
        }

        int displayAmount = GasUnits.toKilo(totalAmount);
        if (displayAmount != getLastDisplaySnapshot(context)) {
            requestStatusToUpdate(displayAmount, context);
        }
        return testLong(operator, totalAmount, targetAmount);
    }

    @Override
    protected Component getUnit() {
        return Component.literal("kGU");
    }

    @Override
    protected ItemStack getIcon() {
        return filterItem;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
        super.initConfigurationWidgets(builder);
        builder.addSelectionScrollInput(71, 50, (input, ignoredLabel) -> input.forOptions(List.of(CCBLang.translateDirect("gui.unit.kilo_gas_units"))).titled(null), "Measure");
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
        filterItem = GasFilters.normalizeStack(savedFilter);
    }

    @Override
    public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag compoundTag) {
        int lastDisplaySnapshot = getLastDisplaySnapshot(compoundTag);
        if (lastDisplaySnapshot == -1) {
            return Component.empty();
        }

        int thresholdOffset = switch (getOperator()) {
            case LESS -> -1;
            case GREATER -> 1;
            case EQUAL -> 0;
        };
        return CCBLang.translateDirect("schedule.condition.threshold.status", lastDisplaySnapshot, Math.max(0, getThreshold() + thresholdOffset), CCBLang.translateDirect("gui.unit.kilo_gas_units"));
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
        else if (GasFilters.isFilter(filterItem)) {
            filterDescription = CCBLang.translateDirect("schedule.condition.threshold.matching_gas_content");
        }
        else {
            filterDescription = CCBLang.translateDirect("schedule.condition.threshold.invalid_gas_filter");
        }
        titleLines.add(CCBLang.translateDirect("schedule.condition.threshold.x_units_of_item", getThreshold(), CCBLang.translateDirect("gui.unit.kilo_gas_units"), filterDescription).withStyle(ChatFormatting.DARK_AQUA));
        return titleLines;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        filterItem = GasFilters.normalizeStack(stack);
    }

    @Override
    public ItemStack getItem(int slot) {
        return filterItem.copy();
    }

    private static boolean testLong(Ops operator, long currentAmount, long targetAmount) {
        return switch (operator) {
            case GREATER -> currentAmount > targetAmount;
            case EQUAL -> currentAmount == targetAmount;
            case LESS -> currentAmount < targetAmount;
        };
    }
}
