package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.CreativeSmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.WrapperMountedGasStorage;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankMountedStorage extends WrapperMountedGasStorage<CreativeSmartGasTank> {
    static final MapCodec<CreativeAirtightTankMountedStorage> CODEC = CreativeSmartGasTank.CODEC.xmap(CreativeAirtightTankMountedStorage::new, storage -> storage.wrapped).fieldOf("value");

    private CreativeAirtightTankMountedStorage(CreativeSmartGasTank tank) {
        this(CCBMountedStorage.CREATIVE_AIRTIGHT_TANK.get(), tank);
    }

    private CreativeAirtightTankMountedStorage(MountedGasStorageType<?> type, CreativeSmartGasTank tank) {
        super(type, tank);
    }

    static CreativeAirtightTankMountedStorage fromTank(CreativeAirtightTankBlockEntity tank) {
        GasTank tankInventory = tank.getTankInventory();
        CreativeSmartGasTank tankCopy = new CreativeSmartGasTank(tankInventory.getCapacity(), ignored -> {});
        tankCopy.setContainedGas(tankInventory.getGasStack());
        return new CreativeAirtightTankMountedStorage(tankCopy);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
    }
}
