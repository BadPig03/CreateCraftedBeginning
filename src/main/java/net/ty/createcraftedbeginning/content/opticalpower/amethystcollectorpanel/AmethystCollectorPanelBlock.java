package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AmethystCollectorPanelBlock extends Block implements OpticalPowerSource {
    static final int MAX_SIDE = 5;
    static final int MAX_AREA = MAX_SIDE * MAX_SIDE;
    private static final Map<AmethystCollectorPanelLayout, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public AmethystCollectorPanelBlock(Properties properties) {
        super(properties);
    }

    private static void refreshModels(Level level, BlockPos origin) {
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-MAX_SIDE, 0, -MAX_SIDE), origin.offset(MAX_SIDE, 0, MAX_SIDE))) {
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AmethystCollectorPanelBlock)) {
                continue;
            }

            level.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS);
        }
    }

    @Override
    public Source getOpticalPowerSource(Level level, BlockPos pos, BlockState state) {
        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.findGeometry(level, pos);
        AmethystCollectorPanelRectangle rectangle = geometry.activeRectangle();
        int powerPoints = geometry.topologyValid() && rectangle != null ? AmethystCollectorPanelRectangle.calculatePowerPoints(level, rectangle) : 0;
        return new Source(geometry.anchor(), powerPoints, geometry.dependencies(), geometry.powerDependencies(), true, geometry.topologyValid());
    }

    @Override
    public int getCurrentOpticalPowerPoints(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        if (!discoveredSource.topologyValid()) {
            return 0;
        }

        AmethystCollectorPanelRectangle rectangle = AmethystCollectorPanelGeometry.rectangleFromKnownValidDependencies(discoveredSource.powerDependencies());
        if (rectangle == null) {
            return 0;
        }

        return AmethystCollectorPanelRectangle.calculatePowerPoints(level, rectangle);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (oldState.is(state.getBlock())) {
            return;
        }

        if (level.isClientSide) {
            refreshModels(level, pos);
            return;
        }

        OpticalPowerNetworkManager.invalidateAmethystCollectorPanelChange(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level.isClientSide && !state.is(newState.getBlock())) {
            refreshModels(level, pos);
        }
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            OpticalPowerNetworkManager.invalidateAmethystCollectorPanelChange(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AmethystCollectorPanelLayout layout = AmethystCollectorPanelLayout.at(level, pos);
        if (layout.equals(AmethystCollectorPanelLayout.SINGLE)) {
            return CCBShapes.AMETHYST_COLLECTOR_PANEL_SHAPE;
        }
        return SHAPES.computeIfAbsent(layout, AmethystCollectorPanelLayout::shape);
    }
}
