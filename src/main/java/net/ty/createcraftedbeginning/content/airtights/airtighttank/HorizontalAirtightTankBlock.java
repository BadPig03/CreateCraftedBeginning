package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HorizontalAirtightTankBlock extends AirtightTankBlock {
    public static final EnumProperty<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public HorizontalAirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_AXIS, Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_AXIS);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockEntityType<? extends AirtightTankBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.HORIZONTAL_AIRTIGHT_TANK.get();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            BlockState placedOn = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
            if (placedOn.getBlock() == this && placedOn.hasProperty(HORIZONTAL_AXIS)) {
                return defaultBlockState().setValue(HORIZONTAL_AXIS, placedOn.getValue(HORIZONTAL_AXIS)).setValue(TOP, true).setValue(BOTTOM, true);
            }
        }

        Axis axis = context.getClickedFace().getAxis();
        if (!axis.isHorizontal()) {
            axis = context.getHorizontalDirection().getAxis();
        }
        return defaultBlockState().setValue(HORIZONTAL_AXIS, axis).setValue(TOP, true).setValue(BOTTOM, true);
    }
}
