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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.data.CCBLang;

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
        int target = getThreshold();
        long foundGas = 0;
        for (Carriage carriage : train.carriages) {
            if (!(carriage.storage instanceof IMountedStorageManagerWithGas withGas)) {
                continue;
            }

            MountedGasStorageWrapper gases = withGas.ccb$getGases();
            for (int i = 0; i < gases.getTanks(); i++) {
                GasStack gasInTank = gases.getGasInTank(i);
                if (!filterItem.isEmpty() && (!(filterItem.getItem() instanceof IGasFilter filter) || !filter.test(filterItem, gasInTank))) {
                    continue;
                }

                foundGas += gasInTank.getAmount();
            }
        }

        int finalGas = Math.clamp(foundGas, 0, Integer.MAX_VALUE);
        requestStatusToUpdate(finalGas / 1000, context);
        return operator.test(finalGas, target * 1000);
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
        builder.addSelectionScrollInput(71, 50, (i, l) -> i.forOptions(List.of(CCBLang.translateDirect("gui.threshold.buckets"))).titled(null), "Measure");
    }

    @Override
    protected void writeAdditional(Provider provider, CompoundTag compoundTag) {
        super.writeAdditional(provider, compoundTag);
        compoundTag.put(COMPOUND_KEY_GAS_FILTER, filterItem.saveOptional(provider));
    }

    @Override
    protected void readAdditional(Provider provider, CompoundTag compoundTag) {
        super.readAdditional(provider, compoundTag);
        if (!compoundTag.contains(COMPOUND_KEY_GAS_FILTER)) {
            return;
        }

        filterItem = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_GAS_FILTER));
    }

    @Override
    public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag tag) {
        int lastDisplaySnapshot = getLastDisplaySnapshot(tag);
        if (lastDisplaySnapshot == -1) {
            return Component.empty();
        }

        int offset = getOperator() == Ops.LESS ? -1 : getOperator() == Ops.GREATER ? 1 : 0;
        return CCBLang.translateDirect("schedule.condition.threshold.status", lastDisplaySnapshot, Math.max(0, getThreshold() + offset), CCBLang.translateDirect("gui.threshold.buckets"));
    }

    @Override
    public ResourceLocation getId() {
        return CreateCraftedBeginning.asResource("gas_threshold");
    }

    @Override
    public List<Component> getTitleAs(String type) {
        List<Component> list = new ArrayList<>();
        list.add(CCBLang.translateDirect("schedule.condition.threshold.train_holds", CCBLang.translateDirect("schedule.condition.threshold." + Lang.asId(getOperator().name()))));
        Component content;
        if (filterItem.isEmpty()) {
            content = CCBLang.translateDirect("schedule.condition.threshold.anything");
        }
        else if (filterItem.getItem() instanceof IGasFilter) {
            content = CCBLang.translateDirect("schedule.condition.threshold.matching_gas_content");
        }
        else {
            content = GasStack.EMPTY.getHoverName();
        }
        list.add(CCBLang.translateDirect("schedule.condition.threshold.x_units_of_item", getThreshold(), CCBLang.translateDirect("gui.threshold.buckets"), content).withStyle(ChatFormatting.DARK_AQUA));
        return list;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        filterItem = stack.copy();
    }

    @Override
    public ItemStack getItem(int slot) {
        return filterItem.copy();
    }
}
