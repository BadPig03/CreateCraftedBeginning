package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetworkManager;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource;
import net.ty.createcraftedbeginning.platform.SkyLightCollisionContext;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AmethystCollectorPanelBlock extends Block implements IBE<AmethystCollectorPanelBlockEntity>, OpticalPowerSource, IWrenchable {
    static final int MAX_SIDE = 5;
    static final int MAX_AREA = MAX_SIDE * MAX_SIDE;
    private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new AmethystCollectorPanelPlacementHelper());
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

    private static VoxelShape shapeForLayout(AmethystCollectorPanelLayout layout) {
        return SHAPES.computeIfAbsent(layout, AmethystCollectorPanelLayout::shape);
    }

    @Override
    public Class<AmethystCollectorPanelBlockEntity> getBlockEntityClass() {
        return AmethystCollectorPanelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AmethystCollectorPanelBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AMETHYST_COLLECTOR_PANEL.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
        if (player.isShiftKeyDown() || !player.mayBuild() || !helper.matchesItem(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        return helper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
    }

    @Override
    public boolean canConnectOpticalPower(BlockState state, Direction side) {
        return side.getAxis().isHorizontal();
    }

    @Override
    public Source resolveOpticalPowerSource(Level level, BlockPos pos, BlockState state) {
        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.scanGeometry(level, pos);
        AmethystCollectorPanelRectangle rectangle = geometry.activeRectangle();
        boolean topologyValid = geometry.topologyValid();
        double fluxLm = topologyValid && rectangle != null ? AmethystCollectorPanelPower.calculateOutput(level, rectangle).fluxLm() : 0;
        return new Source(geometry.anchor(), fluxLm, geometry.dependencies(), geometry.powerDependencies(), true, topologyValid);
    }

    @Override
    public double getCurrentOpticalFluxLm(Level level, BlockPos pos, BlockState state, Source discoveredSource) {
        if (!discoveredSource.topologyValid()) {
            return 0;
        }

        AmethystCollectorPanelRectangle rectangle = AmethystCollectorPanelGeometry.rectangleFromKnownValidDependencies(discoveredSource.powerDependencies());
        if (rectangle == null) {
            return 0;
        }

        return AmethystCollectorPanelPower.calculateOutput(level, rectangle).fluxLm();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }

        withBlockEntityDo(serverLevel, pos, panel -> panel.place(serverLevel, player.getUUID()));
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

        OpticalPowerNetworkManager.invalidateAround(level, pos);
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.restoringBlockSnapshots || !(serverLevel.getChunkAt(pos).getBlockEntity(pos) instanceof AmethystCollectorPanelBlockEntity panel)) {
            return;
        }

        panel.commitPlacement(serverLevel);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level.isClientSide && !state.is(newState.getBlock())) {
            refreshModels(level, pos);
        }
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            OpticalPowerNetworkManager.invalidateAround(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getFluidState().isEmpty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.scanGeometry(level, pos);
        VoxelShape shape = shapeForLayout(AmethystCollectorPanelLayout.at(geometry, pos));
        if (!(context instanceof SkyLightCollisionContext skyLight) || !skyLight.isQueryFor(level) || !geometry.topologyValid()) {
            return shape;
        }

        for (BlockPos dependency : geometry.dependencies()) {
            BlockState dependencyState = level.getBlockState(dependency);
            if (!dependencyState.is(this) || dependencyState.canOcclude()) {
                continue;
            }

            skyLight.cacheShape(level, dependency, shapeForLayout(AmethystCollectorPanelLayout.at(geometry, dependency)));
        }
        return shape;
    }
}
