package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalElement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorBlock extends DirectionalBlock implements IBE<AmethystMirrorBlockEntity>, OpticalElement, IWrenchable {
    public AmethystMirrorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    public static boolean isReflectiveSurface(OpticalSurface surface) {
        return surface.interactor() instanceof AmethystMirrorOptics;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return simpleCodec(AmethystMirrorBlock::new);
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
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity mirror) {
            return mirror.getMirrorOptics().getCollisionShape();
        }

        return CCBShapes.AMETHYST_MIRROR_COLLISION_SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!(context instanceof LaserCollisionContext laser)) {
            if (level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity mirror) {
                return mirror.getPhysicalShape();
            }

            return new AmethystMirrorCollisionShape(AmethystMirrorGeometry.create(state.getValue(FACING).getRotation()));
        }

        OpticalGeometry geometry = getOpticalGeometry(level, pos, state, laser.getPartialTick());
        if (geometry != null) {
            return geometry.getCollisionShape();
        }

        return CCBShapes.AMETHYST_MIRROR_COLLISION_SHAPE;
    }

    @Override
    public Class<AmethystMirrorBlockEntity> getBlockEntityClass() {
        return AmethystMirrorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AmethystMirrorBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AMETHYST_MIRROR.get();
    }

    @Override
    public @Nullable OpticalGeometry getOpticalGeometry(BlockGetter level, BlockPos pos, BlockState state, float partialTick) {
        if (!(level.getBlockEntity(pos) instanceof AmethystMirrorBlockEntity mirror)) {
            return null;
        }

        return mirror.getMirrorOptics();
    }

    @Override
    public OpticalGeometry getMountedOpticalGeometry(BlockState state, @Nullable CompoundTag tag) {
        Direction facing = state.getValue(FACING);
        if (tag == null) {
            return AmethystMirrorOptics.mounted(AmethystMirrorGeometry.createRotation(facing, 0, 0));
        }

        return AmethystMirrorOptics.mounted(AmethystMirrorSerialization.readRotation(facing, tag));
    }
}
