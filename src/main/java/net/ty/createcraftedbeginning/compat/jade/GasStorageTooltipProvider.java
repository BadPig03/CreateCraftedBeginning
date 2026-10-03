package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.jade.gas.GasStorageDataProvider;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility.StorageReadout;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructural;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructural;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeGasContainer;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.behaviour.OverpressureBehaviour;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Snapshot;
import net.ty.createcraftedbeginning.gas.visual.OverpressureTooltips.Status;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasStorageTooltipProvider implements IServerDataProvider<BlockAccessor>, IComponentProvider<BlockAccessor> {
    INSTANCE;

    static final ResourceLocation OVERPRESSURE_TOOLTIP = CCBAPI.asResource("gas_overpressure_tooltip");

    private static final String OVERPRESSURE_DATA_KEY = "JadeOverpressure";
    private static final String OVERPRESSURE_STRESS_KEY = "Stress";
    private static final String OVERPRESSURE_STATUS_KEY = "Status";

    private static @Nullable BlockEntity findStorageOwner(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return null;
        }

        BlockState state = blockEntity.getBlockState();
        BlockPos masterPos;
        Class<? extends BlockEntity> masterType;
        if (blockEntity instanceof AirtightForgingPressStructuralShaftBlockEntity) {
            if (!AirtightForgingPressStructuralShaftBlockEntity.isUpperStore(state)) {
                return null;
            }

            masterPos = AirtightForgingPressStructural.getMaster(blockEntity.getBlockPos(), state);
            masterType = AirtightForgingPressBlockEntity.class;
        }
        else if (blockEntity instanceof AirtightReactorKettleStructuralBlockEntity) {
            if (!AirtightReactorKettleStructuralBlockEntity.canStore(state)) {
                return null;
            }

            masterPos = AirtightReactorKettleStructural.getMaster(blockEntity.getBlockPos(), state);
            masterType = AirtightReactorKettleBlockEntity.class;
        }
        else {
            return blockEntity;
        }

        Level level = blockEntity.getLevel();
        if (level == null || !level.isLoaded(masterPos)) {
            return null;
        }

        BlockEntity master = level.getBlockEntity(masterPos);
        if (!(master != null && !master.isRemoved() && masterType.isInstance(master))) {
            return null;
        }

        return master;
    }

    private static Set<GasHandler> getGasHandlers(Level level, BlockPos pos, BlockEntity blockEntity) {
        Set<GasHandler> gasHandlers = new HashSet<>();
        for (Direction direction : Iterate.directions) {
            GasHandler gasHandler = level.getCapability(GasCapabilities.BLOCK, pos, direction);
            if (gasHandler == null) {
                continue;
            }

            gasHandlers.add(gasHandler);
        }

        if (blockEntity instanceof AirtightForgingPressBlockEntity press) {
            gasHandlers.add(press.getGasCapability());
        }
        return gasHandlers;
    }

    private static void appendOverpressureTooltip(ITooltip tooltip, CompoundTag serverData) {
        if (!serverData.contains(OVERPRESSURE_DATA_KEY)) {
            return;
        }

        CompoundTag overpressureData = NbtValues.getCompoundOrEmpty(serverData, OVERPRESSURE_DATA_KEY);
        if (!overpressureData.contains(OVERPRESSURE_STRESS_KEY) || !overpressureData.contains(OVERPRESSURE_STATUS_KEY)) {
            return;
        }

        int statusOrdinal = overpressureData.getInt(OVERPRESSURE_STATUS_KEY);
        Status[] statuses = Status.values();
        if (statusOrdinal < 0 || statusOrdinal >= statuses.length) {
            return;
        }

        Snapshot snapshot = new Snapshot(overpressureData.getFloat(OVERPRESSURE_STRESS_KEY), statuses[statusOrdinal]);
        Component stressValue = Component.literal(String.valueOf(snapshot.stressPercent()) + '%').withStyle(snapshot.stressColor());
        Component statusValue = Component.translatable("createcraftedbeginning." + snapshot.status().translationKey()).withStyle(snapshot.status().color());
        tooltip.add(Component.empty().append(Component.translatable("createcraftedbeginning.gui.overpressure.structural_stress").withStyle(ChatFormatting.GRAY)).append(Component.literal(": ")).append(stressValue), OVERPRESSURE_TOOLTIP);
        tooltip.add(Component.empty().append(Component.translatable("createcraftedbeginning.gui.overpressure.status").withStyle(ChatFormatting.GRAY)).append(Component.literal(": ")).append(statusValue), OVERPRESSURE_TOOLTIP);
    }

    private static void appendOverpressureServerData(CompoundTag compoundTag, BlockEntity blockEntity) {
        Snapshot snapshot = getOverpressureSnapshot(blockEntity);
        if (snapshot == null) {
            return;
        }

        CompoundTag overpressureData = new CompoundTag();
        overpressureData.putFloat(OVERPRESSURE_STRESS_KEY, snapshot.stress());
        overpressureData.putInt(OVERPRESSURE_STATUS_KEY, snapshot.status().ordinal());
        compoundTag.put(OVERPRESSURE_DATA_KEY, overpressureData);
    }

    private static @Nullable Snapshot getOverpressureSnapshot(BlockEntity blockEntity) {
        if (blockEntity instanceof AirtightTankBlockEntity tank) {
            AirtightTankBlockEntity controller = tank.getControllerBE();
            if (controller == null) {
                return null;
            }

            OverpressureBehaviour overpressure = BlockEntityBehaviour.get(controller, OverpressureBehaviour.TYPE);
            if (overpressure == null) {
                return null;
            }

            return OverpressureTooltips.snapshot(overpressure, controller.getTankInventory());
        }

        OverpressureBehaviour overpressure = BlockEntityBehaviour.get(blockEntity, OverpressureBehaviour.TYPE);
        if (overpressure == null) {
            return null;
        }

        if (blockEntity instanceof AirtightForgingPressBlockEntity press) {
            return OverpressureTooltips.snapshot(overpressure, press.getGasCapability());
        }

        if (blockEntity instanceof AirtightReactorKettleBlockEntity kettle) {
            return OverpressureTooltips.snapshot(overpressure, kettle.getAvailableGases());
        }

        if (!(blockEntity instanceof GasInjectionChamberBlockEntity chamber)) {
            return null;
        }

        SmartGasTankBehaviour tankBehaviour = BlockEntityBehaviour.get(chamber, SmartGasTankBehaviour.TYPE);
        if (tankBehaviour == null) {
            return null;
        }

        return OverpressureTooltips.snapshot(overpressure, tankBehaviour.getCapability());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(GasStorageDataProvider.STORAGE_KEY) || !serverData.contains(GasStorageDataProvider.STORAGE_UID_KEY) || !JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP.toString().equals(serverData.getString(GasStorageDataProvider.STORAGE_UID_KEY))) {
            return;
        }

        BlockEntity storageOwner = findStorageOwner(accessor.getBlockEntity());
        if (storageOwner == null) {
            return;
        }

        StorageReadout readout = AirtightTelemetryVisibility.storageReadout(storageOwner);
        GasStorageDataProvider.appendData(tooltip, serverData, accessor.showDetails(), readout.capacity(), readout.pressure());
        appendOverpressureTooltip(tooltip, serverData);
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        Level level = blockAccessor.getLevel();
        BlockPos pos = blockAccessor.getPosition();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity instanceof TeslaTurbineNozzleBlockEntity) {
            return;
        }

        Set<GasHandler> gasHandlers = getGasHandlers(level, pos, blockEntity);
        if (gasHandlers.isEmpty()) {
            return;
        }

        BlockEntity storageOwner = findStorageOwner(blockEntity);
        if (storageOwner == null) {
            return;
        }

        boolean isCreative = blockEntity instanceof CreativeGasContainer container && container.isCreative(level, level.getBlockState(pos), pos);
        StorageReadout readout = AirtightTelemetryVisibility.storageReadout(storageOwner);
        GasStorageDataProvider.readData(compoundTag, gasHandlers, JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP, isCreative, readout.capacity(), readout.pressure());
        appendOverpressureServerData(compoundTag, storageOwner);
    }

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP;
    }
}
