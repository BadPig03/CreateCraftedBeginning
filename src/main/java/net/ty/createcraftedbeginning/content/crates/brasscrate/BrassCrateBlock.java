package net.ty.createcraftedbeginning.content.crates.brasscrate;

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
public class BrassCrateBlock extends CrateBlock<BrassCrateBlockEntity> {
    public BrassCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(BrassCrateBlock::new);
    }

    @Override
    public Class<BrassCrateBlockEntity> getBlockEntityClass() {
        return BrassCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BrassCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.BRASS_CRATE.get();
    }

    @Override
    protected void onCrateRemoved(Level level, BlockPos pos, BrassCrateBlockEntity crate, boolean isMoving) {
        if (isMoving || level.isClientSide) {
            return;
        }

        super.onCrateRemoved(level, pos, crate, false);
        crate.destroy();
    }
}
