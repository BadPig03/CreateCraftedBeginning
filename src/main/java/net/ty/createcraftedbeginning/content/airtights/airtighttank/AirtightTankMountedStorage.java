package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMountedStorage.Handler;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.gas.mounted.WrapperMountedGasStorage;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankMountedStorage extends WrapperMountedGasStorage<Handler> implements SyncedMountedStorage {
    static final MapCodec<AirtightTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.LONG.fieldOf("volume").forGetter(AirtightTankMountedStorage::getVolume), Codec.LONG.optionalFieldOf("max_pressure", GasPressure.REFERENCE_PRESSURE_PA).forGetter(AirtightTankMountedStorage::getMaxPressurePa), GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(AirtightTankMountedStorage::getGasStack)).apply(instance, AirtightTankMountedStorage::new));

    private boolean dirty;

    private AirtightTankMountedStorage(long volume, long maxPressurePa, GasStack gasStack) {
        this(new GasTankState(new GasTankLimits(volume, maxPressurePa), gasStack));
    }

    private AirtightTankMountedStorage(GasTankState state) {
        this(CCBMountedStorage.AIRTIGHT_TANK.get(), state);
    }

    private AirtightTankMountedStorage(MountedGasStorageType<?> type, GasTankState state) {
        super(type, new Handler(state));
        wrapped.onChange = () -> dirty = true;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof AirtightTankBlockEntity tank) || !tank.isController()) {
            return;
        }

        tank.getTankInventory().tryApplyState(getState()).requireAccepted();
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void markClean() {
        dirty = false;
    }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
        if (!(contraption.getBlockEntityClientSide(localPos) instanceof AirtightTankBlockEntity tank)) {
            return;
        }

        tank.getTankInventory().tryApplyState(getState()).requireAccepted();
    }

    @Contract("_ -> new")
    static AirtightTankMountedStorage fromTank(AirtightTankBlockEntity tank) {
        return new AirtightTankMountedStorage(tank.getTankInventory().snapshot());
    }

    private long getVolume() {
        return wrapped.getVolume();
    }

    private long getMaxPressurePa() {
        return wrapped.getMaxPressurePa();
    }

    private GasStack getGasStack() {
        return wrapped.getGasStack();
    }

    private GasTankState getState() {
        return wrapped.snapshot();
    }

    static final class Handler extends GasTank {
        private Runnable onChange = () -> {
        };

        private Handler(GasTankState state) {
            super(state.limits());
            tryApplyState(state).requireAccepted();
        }

        @Override
        protected void onStateChanged() {
            onChange.run();
        }
    }
}
