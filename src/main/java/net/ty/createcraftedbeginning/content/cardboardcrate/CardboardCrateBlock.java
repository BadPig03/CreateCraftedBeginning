package net.ty.createcraftedbeginning.content.cardboardcrate;

import com.simibubi.create.content.logistics.crate.CrateBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class CardboardCrateBlock extends CrateBlock implements IBE<CardboardCrateBlockEntity> {
    static final int MAX_SLOT = 2;
    public static final int SLOT_LIMIT = 64;

    public CardboardCrateBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    public Class<CardboardCrateBlockEntity> getBlockEntityClass() {
        return CardboardCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CardboardCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CARDBOARD_CRATE.get();
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
