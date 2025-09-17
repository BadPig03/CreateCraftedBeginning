package net.ty.createcraftedbeginning.api.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import org.jetbrains.annotations.Nullable;

public class GasTankMountedStorageType extends MountedGasStorageType<GasTankMountedStorage>{
    public GasTankMountedStorageType() {
		super(GasTankMountedStorage.CODEC);
	}

	@Override
	@Nullable
	public GasTankMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		if (be instanceof AirtightTankBlockEntity tank && tank.isController()) {
			return GasTankMountedStorage.fromTank(tank);
		}
        if (be instanceof CreativeAirtightTankBlockEntity tank && tank.isController()) {
			return GasTankMountedStorage.fromTank(tank);
		}

		return null;
	}
}
