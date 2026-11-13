package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalElement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystSoundSuppressorBlock extends Block implements IBE<AmethystSoundSuppressorBlockEntity>, IWrenchable, OpticalElement {
    public AmethystSoundSuppressorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.TOOLS_WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        withBlockEntityDo(level, pos, AmethystSoundSuppressorBlockEntity::toggleShowOutline);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AMETHYST_SOUND_SUPPRESSOR_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof LaserCollisionContext) {
            return AmethystSoundSuppressorPort.INSTANCE.getCollisionShape();
        }

        return CCBShapes.AMETHYST_SOUND_SUPPRESSOR_COLLISION_SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }

        withBlockEntityDo(serverLevel, pos, suppressor -> suppressor.place(serverLevel, player.getUUID()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moved) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.restoringBlockSnapshots || previous.is(this)) {
            return;
        }

        if (!(serverLevel.getChunkAt(pos).getBlockEntity(pos) instanceof AmethystSoundSuppressorBlockEntity suppressor)) {
            return;
        }

        suppressor.commitPlacement(serverLevel);
    }

    @Override
    public Class<AmethystSoundSuppressorBlockEntity> getBlockEntityClass() {
        return AmethystSoundSuppressorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AmethystSoundSuppressorBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AMETHYST_SOUND_SUPPRESSOR.get();
    }

    @Override
    public OpticalGeometry getOpticalGeometry(BlockGetter level, BlockPos pos, BlockState state, float partialTick) {
        return AmethystSoundSuppressorPort.INSTANCE;
    }

    @Override
    public OpticalGeometry getMountedOpticalGeometry(BlockState state, @Nullable CompoundTag tag) {
        return AmethystSoundSuppressorPort.INSTANCE;
    }
}
