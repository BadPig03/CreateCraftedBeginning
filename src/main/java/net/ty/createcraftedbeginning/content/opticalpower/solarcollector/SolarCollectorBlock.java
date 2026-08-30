package net.ty.createcraftedbeginning.content.opticalpower.solarcollector;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SolarCollectorBlock extends Block implements OpticalPowerSource {
    static final int MAX_SIDE = 5;
    static final int MAX_AREA = MAX_SIDE * MAX_SIDE;

    public SolarCollectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Source getOpticalPowerSource(Level level, BlockPos pos, BlockState state) {
        SolarCollectorGeometry geometry = SolarCollectorGeometry.findGeometry(level, pos);
        SolarCollectorRectangle rectangle = geometry.activeRectangle();
        int powerPoints = geometry.topologyValid() && rectangle != null ? SolarCollectorRectangle.calculatePowerPoints(level, rectangle) : 0;
        return new Source(geometry.anchor(), powerPoints, geometry.dependencies(), geometry.powerDependencies(), true, geometry.topologyValid());
    }

    @Override
    public int getCurrentOpticalPowerPoints(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        if (!discoveredSource.topologyValid()) {
            return 0;
        }

        SolarCollectorRectangle rectangle = SolarCollectorGeometry.rectangleFromKnownValidDependencies(discoveredSource.powerDependencies());
        if (rectangle == null) {
            return 0;
        }
        return SolarCollectorRectangle.calculatePowerPoints(level, rectangle);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide || oldState.is(state.getBlock())) {
            return;
        }

        OpticalPowerNetworkManager.invalidateSolarCollectorChange(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            OpticalPowerNetworkManager.invalidateSolarCollectorChange(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.SOLAR_COLLECTOR_SHAPE;
    }
}
