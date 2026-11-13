package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.OpticalPlatePlacement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalElement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLensBlock extends DirectionalBlock implements IBE<AmethystLensBlockEntity>, OpticalElement, IWrenchable {
    private final boolean converging;

    public AmethystLensBlock(Properties properties, boolean converging) {
        super(properties);
        this.converging = converging;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(properties -> new AmethystLensBlock(properties, converging));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, OpticalPlatePlacement.getNormal(context));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return AmethystLensGeometry.forFacing(state.getValue(FACING)).physicalShape();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        AmethystLensGeometry geometry = AmethystLensGeometry.forFacing(state.getValue(FACING));
        if (context instanceof LaserCollisionContext) {
            return geometry.opticalShape();
        }

        return geometry.physicalShape();
    }

    @Override
    public Class<AmethystLensBlockEntity> getBlockEntityClass() {
        return AmethystLensBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AmethystLensBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AMETHYST_LENS.get();
    }

    @Override
    public @Nullable OpticalGeometry getOpticalGeometry(BlockGetter level, BlockPos pos, BlockState state, float partialTick) {
        if (!(level.getBlockEntity(pos) instanceof AmethystLensBlockEntity lens)) {
            return null;
        }

        return new AmethystLensOptics(AmethystLensGeometry.forFacing(state.getValue(FACING)), lens.getFocalLength(), converging);
    }

    @Override
    public OpticalGeometry getMountedOpticalGeometry(BlockState state, @Nullable CompoundTag tag) {
        return new AmethystLensOptics(AmethystLensGeometry.forFacing(state.getValue(FACING)), AmethystLensSerialization.readFocalLength(tag), converging);
    }
}
