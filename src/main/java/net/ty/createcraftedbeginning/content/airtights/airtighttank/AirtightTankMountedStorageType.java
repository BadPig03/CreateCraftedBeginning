package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import org.jetbrains.annotations.Nullable;

public class AirtightTankMountedStorageType extends MountedGasStorageType<AirtightTankMountedStorage> {
    public AirtightTankMountedStorageType() {
        super(AirtightTankMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public AirtightTankMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof AirtightTankBlockEntity tank) || !tank.isController()) {
            return null;
        }

        return AirtightTankMountedStorage.fromTank(tank);
    }
}