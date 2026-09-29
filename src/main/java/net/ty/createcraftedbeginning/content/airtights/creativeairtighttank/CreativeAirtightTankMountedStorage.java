package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.gas.mounted.WrapperMountedGasStorage;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankMountedStorage extends WrapperMountedGasStorage<CreativeGasReservoir> {
    static final MapCodec<CreativeAirtightTankMountedStorage> CODEC = CreativeGasReservoir.CODEC.xmap(CreativeAirtightTankMountedStorage::new, storage -> storage.wrapped).fieldOf("value");

    private CreativeAirtightTankMountedStorage(CreativeGasReservoir tank) {
        this(CCBMountedStorage.CREATIVE_AIRTIGHT_TANK.get(), tank);
    }

    private CreativeAirtightTankMountedStorage(MountedGasStorageType<?> type, CreativeGasReservoir tank) {
        super(type, tank);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity blockEntity) {
    }

    static CreativeAirtightTankMountedStorage fromTank(CreativeAirtightTankBlockEntity tank) {
        CreativeGasReservoir tankInventory = tank.getTankInventory();
        CreativeGasReservoir tankCopy = new CreativeGasReservoir(tankInventory.getLimits(), tankInventory.getFixedPressurePa(), () -> {});
        tankCopy.setContainedGas(tankInventory.getGasStack());
        return new CreativeAirtightTankMountedStorage(tankCopy);
    }
}
