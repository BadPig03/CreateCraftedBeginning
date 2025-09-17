package net.ty.createcraftedbeginning.api.gas;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.data.CCBSerializerHelper;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GasTankMountedStorage extends WrapperMountedGasStorage<GasTankMountedStorage.Handler> implements SyncedMountedStorage {
    public static final MapCodec<GasTankMountedStorage> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(CCBSerializerHelper.NON_NEGATIVE_LONG_CODEC.fieldOf("capacity").forGetter(GasTankMountedStorage::getCapacity), GasStack.OPTIONAL_CODEC.fieldOf("fluid").forGetter(GasTankMountedStorage::getGas)).apply(i, GasTankMountedStorage::new));

    private boolean dirty;

    protected GasTankMountedStorage(MountedGasStorageType<?> type, long capacity, GasStack stack) {
        super(type, new Handler(capacity, stack));
        this.wrapped.onChange = () -> this.dirty = true;
    }

    protected GasTankMountedStorage(long capacity, GasStack stack) {
        this(CCBMountedStorage.GAS_TANK.get(), capacity, stack);
    }

    @Contract("_ -> new")
    public static @NotNull GasTankMountedStorage fromTank(@NotNull AirtightTankBlockEntity tank) {
        GasTank inventory = tank.getTankInventory();
        return new GasTankMountedStorage(inventory.getCapacity(), inventory.getGas().copy());
    }

    @Contract("_ -> new")
    public static @NotNull GasTankMountedStorage fromTank(@NotNull CreativeAirtightTankBlockEntity tank) {
        GasTank inventory = tank.getTankInventory();
        return new GasTankMountedStorage(inventory.getCapacity(), inventory.getGas().copy());
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof AirtightTankBlockEntity tank && tank.isController()) {
            GasTank inventory = tank.getTankInventory();
            inventory.setGas(this.wrapped.getGas());
        }
    }

    public GasStack getGas() {
        return this.wrapped.getGas();
    }

    public long getCapacity() {
        return this.wrapped.getCapacity();
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void markClean() {
        this.dirty = false;
    }

    @Override
    public void afterSync(@NotNull Contraption contraption, BlockPos localPos) {
        BlockEntity be = contraption.presentBlockEntities.get(localPos);
        if (!(be instanceof AirtightTankBlockEntity tank)) {
            return;
        }

        GasTank inv = tank.getTankInventory();
        inv.setGas(this.getGas());
    }

    public static final class Handler extends GasTank {
        private Runnable onChange = () -> {
        };

        public Handler(long capacity, GasStack stack) {
            super(capacity);
            this.setGas(stack);
        }

        @Override
        protected void onContentsChanged() {
            this.onChange.run();
        }
    }
}
