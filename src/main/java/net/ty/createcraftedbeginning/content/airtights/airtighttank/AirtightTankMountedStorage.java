package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import net.ty.createcraftedbeginning.api.gas.WrapperMountedGasStorage;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankMountedStorage.Handler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirtightTankMountedStorage extends WrapperMountedGasStorage<Handler> implements SyncedMountedStorage {
    public static final MapCodec<AirtightTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codec.LONG.fieldOf("capacity").forGetter(AirtightTankMountedStorage::getCapacity), GasStack.OPTIONAL_CODEC.fieldOf("gas").forGetter(AirtightTankMountedStorage::getGasStack)).apply(i, AirtightTankMountedStorage::new));

    private boolean dirty;

    protected AirtightTankMountedStorage(long capacity, GasStack stack) {
        this(CCBMountedStorage.AIRTIGHT_TANK.get(), capacity, stack);
    }

    protected AirtightTankMountedStorage(MountedGasStorageType<?> type, long capacity, GasStack stack) {
        super(type, new Handler(capacity, stack));
        wrapped.onChange = () -> dirty = true;
    }

    @Contract("_ -> new")
    public static @NotNull AirtightTankMountedStorage fromTank(@NotNull AirtightTankBlockEntity tank) {
        GasTank inventory = tank.getTankInventory();
        return new AirtightTankMountedStorage(inventory.getCapacity(), inventory.getGasStack().copy());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof AirtightTankBlockEntity tank) || !tank.isController()) {
            return;
        }
        
        GasTank inventory = tank.getTankInventory();
        inventory.setGasStack(wrapped.getGasStack());
    }

    public long getCapacity() {
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
    public void afterSync(@NotNull Contraption contraption, BlockPos localPos) {
        if (!(contraption.getBlockEntityClientSide(localPos) instanceof AirtightTankBlockEntity tank)) {
            return;
        }

        tank.getTankInventory().setGasStack(getGasStack());
    }

    public GasStack getGasStack() {
        return wrapped.getGasStack();
    }

    public static final class Handler extends GasTank {
        private Runnable onChange = () -> {
        };

        public Handler(long capacity, GasStack stack) {
            super(capacity);
            setGasStack(stack);
        }

        @Override
        protected void onContentsChanged() {
            onChange.run();
        }
    }
}
