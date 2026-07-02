package net.ty.createcraftedbeginning.api.gas.coolantstrategy;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity.CoolantEfficiency;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface CoolantStrategyHandler {
    SimpleRegistry<Block, CoolantStrategyHandler> REGISTRY = SimpleRegistry.create();

    CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState);

    @Nullable
    BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState);
}
