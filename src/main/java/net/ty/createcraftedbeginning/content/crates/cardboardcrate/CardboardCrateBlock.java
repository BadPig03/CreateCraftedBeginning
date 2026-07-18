package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.content.crates.CrateBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CardboardCrateBlock extends CrateBlock<CardboardCrateBlockEntity> {
    public CardboardCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(CardboardCrateBlock::new);
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
    protected void onCrateRemoved(Level level, BlockPos pos, CardboardCrateBlockEntity crate, boolean isMoving) {
        if (isMoving || level.isClientSide) {
            return;
        }

        crate.awardStoredPackageDisposal();
    }
}
