package net.ty.createcraftedbeginning.content.airtights;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightFlowmeterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTelemetryVisibility {
    private AirtightTelemetryVisibility() {
    }

    public static PipeReadout pipeReadout(@Nullable BlockEntity blockEntity) {
        return switch (blockEntity) {
            case AirtightManometerBlockEntity ignored -> PipeReadout.PRESSURE;
            case AirtightFlowmeterBlockEntity ignored -> PipeReadout.FLOW;
            case AirtightPumpBlockEntity ignored -> PipeReadout.PUMP_CONFIGURATION;
            case AirtightRegulatorPumpBlockEntity ignored -> PipeReadout.REGULATOR_CONFIGURATION;
            case AirtightValveBlockEntity ignored -> PipeReadout.VALVE_STATE;
            case null, default -> PipeReadout.NONE;
        };
    }

    public static boolean canReadPipePressure(@Nullable BlockEntity blockEntity) {
        return pipeReadout(blockEntity) == PipeReadout.PRESSURE;
    }

    public static boolean canReadPipeFlow(@Nullable BlockEntity blockEntity) {
        return pipeReadout(blockEntity) == PipeReadout.FLOW;
    }

    public static StorageReadout storageReadout(@Nullable BlockEntity blockEntity) {
        return switch (blockEntity) {
            case CreativeAirtightTankBlockEntity ignored -> StorageReadout.PRESSURE_ONLY;
            case AirtightTankBlockEntity tank when tank.hasTankGauge() -> StorageReadout.PRESSURE_ONLY;
            case GasInjectionChamberBlockEntity ignored -> StorageReadout.PRESSURE_ONLY;
            case AirtightFractionationTowerBlockEntity ignored -> StorageReadout.PRESSURE_ONLY;
            case AirtightReactorKettleBlockEntity ignored -> StorageReadout.PRESSURE_ONLY;
            case AirtightReactorKettleStructuralBlockEntity ignored -> StorageReadout.PRESSURE_ONLY;
            case AirtightForgingPressBlockEntity ignored -> StorageReadout.PRESSURE_ONLY;
            case null, default -> StorageReadout.AMOUNT_ONLY;
        };
    }

    public static boolean canReadStoragePressure(@Nullable BlockEntity blockEntity) {
        return storageReadout(blockEntity).pressure();
    }

    public static boolean canReadStorageCapacity(@Nullable BlockEntity blockEntity) {
        return storageReadout(blockEntity).capacity();
    }

    public enum PipeReadout {
        NONE,
        PRESSURE,
        FLOW,
        PUMP_CONFIGURATION,
        REGULATOR_CONFIGURATION,
        VALVE_STATE
    }

    public enum StorageReadout {
        AMOUNT_ONLY(false, false),
        PRESSURE_ONLY(false, true),
        FULL(true, true);

        private final boolean capacity;
        private final boolean pressure;

        StorageReadout(boolean capacity, boolean pressure) {
            this.capacity = capacity;
            this.pressure = pressure;
        }

        public boolean capacity() {
            return capacity;
        }

        public boolean pressure() {
            return pressure;
        }
    }
}
