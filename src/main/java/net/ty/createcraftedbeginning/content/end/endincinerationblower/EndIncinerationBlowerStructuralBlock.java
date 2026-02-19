package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EndIncinerationBlowerStructuralBlock extends KineticBlock implements IBE<EndIncinerationBlowerStructuralBlockEntity> {
    public EndIncinerationBlowerStructuralBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return 15;
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (!(level.getBlockEntity(pos) instanceof EndIncinerationBlowerStructuralBlockEntity)) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(CCBBlocks.END_CASING_BLOCK);
    }

    @Override
    public @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull Builder builder) {
        List<ItemStack> lootDrops = super.getDrops(state, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof EndIncinerationBlowerStructuralBlockEntity)) {
            return lootDrops;
        }

        return List.of(new ItemStack(CCBBlocks.END_CASING_BLOCK));
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, @NotNull Direction direction) {
        return direction.getAxis() == Axis.Y;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    @Override
    public Class<EndIncinerationBlowerStructuralBlockEntity> getBlockEntityClass() {
        return EndIncinerationBlowerStructuralBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndIncinerationBlowerStructuralBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.END_INCINERATION_BLOWER_STRUCTURAL.get();
    }
}
