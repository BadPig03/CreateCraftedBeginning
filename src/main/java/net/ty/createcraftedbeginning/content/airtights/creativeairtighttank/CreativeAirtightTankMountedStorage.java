package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import net.ty.createcraftedbeginning.api.gas.WrapperMountedGasStorage;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreativeAirtightTankMountedStorage extends WrapperMountedGasStorage<CreativeSmartGasTank> {
    public static final MapCodec<CreativeAirtightTankMountedStorage> CODEC = CreativeSmartGasTank.CODEC.xmap(CreativeAirtightTankMountedStorage::new, storage -> storage.wrapped).fieldOf("value");

    protected CreativeAirtightTankMountedStorage(CreativeSmartGasTank tank) {
        this(CCBMountedStorage.CREATIVE_AIRTIGHT_TANK.get(), tank);
    }

    protected CreativeAirtightTankMountedStorage(MountedGasStorageType<?> type, CreativeSmartGasTank tank) {
		super(type, tank);
	}

    @Contract("_ -> new")
    public static @NotNull CreativeAirtightTankMountedStorage fromTank(@NotNull CreativeAirtightTankBlockEntity tank) {
        GasTank inventory = tank.getTankInventory();
        CreativeSmartGasTank copy = new CreativeSmartGasTank(inventory.getCapacity(), $ -> {});
        copy.setContainedGas(inventory.getGasStack());
        return new CreativeAirtightTankMountedStorage(copy);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
    }

    public long getCapacity() {
        return wrapped.getCapacity();
    }

    public GasStack getGasStack() {
        return wrapped.getGasStack();
    }
}
