package net.ty.createcraftedbeginning.content.airtighttank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class AirtightTankBlock extends Block implements IBE<AirtightTankBlockEntity>, IWrenchable {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final SoundType SILENCED_METAL = new DeferredSoundType(0.1F, 1.5F, () -> SoundEvents.METAL_BREAK, () -> SoundEvents.METAL_STEP, () -> SoundEvents.METAL_PLACE, () -> SoundEvents.METAL_HIT, () -> SoundEvents.METAL_FALL);

    public AirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
    }

    @Override
    public void onPlace(BlockState state, @NotNull Level world, @NotNull BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }
        withBlockEntityDo(world, pos, AirtightTankBlockEntity::updateConnectivity);
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof AirtightTankBlockEntity tankBE)) {
                return;
            }
            world.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(tankBE);
        }
    }

    @Override
    public Class<AirtightTankBlockEntity> getBlockEntityClass() {
        return AirtightTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightTankBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_TANK.get();
    }

    @Override
    public @NotNull SoundType getSoundType(@NotNull BlockState state, @NotNull LevelReader world, @NotNull BlockPos pos, Entity entity) {
        SoundType soundType = super.getSoundType(state, world, pos, entity);
        if (entity != null && entity.getPersistentData().contains("SilenceTankSound")) {
            return SILENCED_METAL;
        }
        return soundType;
    }
}
