package net.ty.createcraftedbeginning.content.airtightpump;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class AirtightPumpBlock extends PumpBlock {
    public AirtightPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
        super.tick(state, world, pos, r);
        AirtightPumpBlockEntity be = (AirtightPumpBlockEntity) this.getBlockEntity(world, pos);
        if (be != null) {
            be.updatePressureChange();
            world.scheduleTick(pos, state.getBlock(), 20);
        }
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return CCBShapes.AIRTIGHT_PUMP.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<PumpBlockEntity> getBlockEntityClass() {
        return (Class<PumpBlockEntity>) (Class<?>) AirtightPumpBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_PUMP.get();
    }
}
