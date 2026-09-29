package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirtightTankMountedStorageType extends MountedGasStorageType<CreativeAirtightTankMountedStorage> {
    public CreativeAirtightTankMountedStorageType() {
        super(CreativeAirtightTankMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public CreativeAirtightTankMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank) || !tank.isController()) {
            return null;
        }

        return CreativeAirtightTankMountedStorage.fromTank(tank);
    }
}