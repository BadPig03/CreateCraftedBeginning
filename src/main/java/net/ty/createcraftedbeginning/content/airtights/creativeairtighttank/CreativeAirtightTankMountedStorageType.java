package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import org.jetbrains.annotations.Nullable;

public class CreativeAirtightTankMountedStorageType extends MountedGasStorageType<CreativeAirtightTankMountedStorage> {
    public CreativeAirtightTankMountedStorageType() {
        super(CreativeAirtightTankMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public CreativeAirtightTankMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof CreativeAirtightTankBlockEntity tank)) {
            return null;
        }

        return CreativeAirtightTankMountedStorage.fromTank(tank);
    }
}