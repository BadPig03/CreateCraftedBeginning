package net.ty.createcraftedbeginning.content.crates.andesitecrate;

import com.mojang.serialization.MapCodec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.content.crates.CrateBlock;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AndesiteCrateBlock extends CrateBlock<AndesiteCrateBlockEntity> {
    public AndesiteCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(AndesiteCrateBlock::new);
    }

    @Override
    public Class<AndesiteCrateBlockEntity> getBlockEntityClass() {
        return AndesiteCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AndesiteCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.ANDESITE_CRATE.get();
    }
}
