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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMountedStorage.Handler;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.WrapperMountedGasStorage;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightTankMountedStorage extends WrapperMountedGasStorage<Handler> implements SyncedMountedStorage {
    static final MapCodec<AirtightTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Codec.LONG.fieldOf("capacity").forGetter(AirtightTankMountedStorage::getCapacity), GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(AirtightTankMountedStorage::getGasStack)).apply(instance, AirtightTankMountedStorage::new));

    private boolean dirty;

    private AirtightTankMountedStorage(long capacity, GasStack gasStack) {
        this(CCBMountedStorage.AIRTIGHT_TANK.get(), capacity, gasStack);
    }

    private AirtightTankMountedStorage(MountedGasStorageType<?> type, long capacity, GasStack gasStack) {
        super(type, new Handler(capacity, gasStack));
        wrapped.onChange = () -> dirty = true;
    }

    @Contract("_ -> new")
    static AirtightTankMountedStorage fromTank(AirtightTankBlockEntity tank) {
        GasTank tankInventory = tank.getTankInventory();
        return new AirtightTankMountedStorage(tankInventory.getCapacity(), tankInventory.getGasStack().copy());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof AirtightTankBlockEntity tank) || !tank.isController()) {
            return;
        }

        tank.getTankInventory().setGasStack(wrapped.getGasStack());
    }

    private long getCapacity() {
        return wrapped.getCapacity();
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

        tank.getTankInventory().setGasStack(getGasStack());
    }

    private GasStack getGasStack() {
        return wrapped.getGasStack();
    }

    static final class Handler extends GasTank {
        private Runnable onChange = () -> {
        };

        private Handler(long capacity, GasStack gasStack) {
            super(capacity);
            setGasStack(gasStack);
        }

        @Override
        protected void onContentsChanged() {
            onChange.run();
        }
    }
}
