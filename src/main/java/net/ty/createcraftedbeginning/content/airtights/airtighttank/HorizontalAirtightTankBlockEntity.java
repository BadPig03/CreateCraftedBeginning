package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizontalAirtightTankBlockEntity extends AirtightTankBlockEntity {
    public HorizontalAirtightTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.HORIZONTAL_AIRTIGHT_TANK.get(), (tank, ignoredDirection) -> tank.getCapability());
    }

    @Override
    public Axis getMainConnectionAxis() {
        BlockState tankState = getBlockState();
        if (tankState.hasProperty(HorizontalAirtightTankBlock.HORIZONTAL_AXIS)) {
            return tankState.getValue(HorizontalAirtightTankBlock.HORIZONTAL_AXIS);
        }
        return Axis.X;
    }
}
