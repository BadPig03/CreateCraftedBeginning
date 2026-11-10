package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.jade.gas.GasStorageDataProvider;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.visual.CreativeGasContainer;
import net.ty.createcraftedbeginning.gas.visual.GasStorageObservationSource;
import net.ty.createcraftedbeginning.gas.visual.GasStorageReadout;
import net.ty.createcraftedbeginning.gas.visual.GasStorageReadoutSource;
import net.ty.createcraftedbeginning.gas.visual.OverpressureReadoutSource;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Snapshot;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Status;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasStorageTooltipProvider implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation OVERPRESSURE_TOOLTIP = CCBAPI.asResource("gas_overpressure_tooltip");

    private static final String COMPOUND_KEY_OVERPRESSURE_DATA = "JadeOverpressure";
    private static final String COMPOUND_KEY_OVERPRESSURE_STRESS = "Stress";
    private static final String COMPOUND_KEY_OVERPRESSURE_STATUS = "Status";

    private static void appendOverpressureTooltip(ITooltip tooltip, CompoundTag serverData) {
        if (!serverData.contains(COMPOUND_KEY_OVERPRESSURE_DATA)) {
            return;
        }

        CompoundTag overpressureData = NbtValues.getCompoundOrEmpty(serverData, COMPOUND_KEY_OVERPRESSURE_DATA);
        if (!overpressureData.contains(COMPOUND_KEY_OVERPRESSURE_STRESS) || !overpressureData.contains(COMPOUND_KEY_OVERPRESSURE_STATUS)) {
            return;
        }

        int statusOrdinal = overpressureData.getInt(COMPOUND_KEY_OVERPRESSURE_STATUS);
        Status[] statuses = Status.values();
        if (statusOrdinal < 0 || statusOrdinal >= statuses.length) {
            return;
        }

        Snapshot snapshot = new Snapshot(overpressureData.getFloat(COMPOUND_KEY_OVERPRESSURE_STRESS), statuses[statusOrdinal]);
        Component stressValue = Component.literal(String.valueOf(snapshot.stressPercent()) + '%').withStyle(snapshot.stressColor());
        Status status = snapshot.status();
        Component statusValue = Component.translatable("createcraftedbeginning." + status.translationKey()).withStyle(status.color());
        tooltip.add(Component.empty().append(Component.translatable("createcraftedbeginning.gui.overpressure.structural_stress").withStyle(ChatFormatting.GRAY)).append(Component.literal(": ")).append(stressValue), OVERPRESSURE_TOOLTIP);
        tooltip.add(Component.empty().append(Component.translatable("createcraftedbeginning.gui.overpressure.status").withStyle(ChatFormatting.GRAY)).append(Component.literal(": ")).append(statusValue), OVERPRESSURE_TOOLTIP);
    }

    private static void appendOverpressureServerData(CompoundTag compoundTag, BlockEntity blockEntity) {
        Snapshot snapshot = OverpressureReadoutSource.readout(blockEntity);
        if (snapshot == null) {
            return;
        }

        CompoundTag overpressureData = new CompoundTag();
        overpressureData.putFloat(COMPOUND_KEY_OVERPRESSURE_STRESS, snapshot.stress());
        overpressureData.putInt(COMPOUND_KEY_OVERPRESSURE_STATUS, snapshot.status().ordinal());
        compoundTag.put(COMPOUND_KEY_OVERPRESSURE_DATA, overpressureData);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(GasStorageDataProvider.COMPOUND_KEY_STORAGE) || !serverData.contains(GasStorageDataProvider.COMPOUND_KEY_STORAGE_UID) || !JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP.toString().equals(serverData.getString(GasStorageDataProvider.COMPOUND_KEY_STORAGE_UID))) {
            return;
        }

        BlockEntity storageOwner = GasStorageObservationSource.owner(accessor.getBlockEntity());
        if (storageOwner == null) {
            return;
        }

        GasStorageReadout readout = GasStorageReadoutSource.readout(storageOwner);
        GasStorageDataProvider.appendData(tooltip, serverData, accessor.showDetails(), readout.capacity(), readout.pressure());
        appendOverpressureTooltip(tooltip, serverData);
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getPosition();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }

        Set<GasHandler> gasHandlers = GasStorageObservationSource.handlers(level, pos, blockEntity);
        if (gasHandlers.isEmpty()) {
            return;
        }

        BlockEntity storageOwner = GasStorageObservationSource.owner(blockEntity);
        if (storageOwner == null) {
            return;
        }

        boolean isCreative = blockEntity instanceof CreativeGasContainer container && container.isCreative(level, level.getBlockState(pos), pos);
        GasStorageReadout readout = GasStorageReadoutSource.readout(storageOwner);
        GasStorageDataProvider.readData(compoundTag, gasHandlers, JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP, isCreative, readout.capacity(), readout.pressure());
        appendOverpressureServerData(compoundTag, storageOwner);
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP;
    }
}
