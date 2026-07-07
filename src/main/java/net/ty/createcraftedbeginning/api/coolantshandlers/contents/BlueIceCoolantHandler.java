package net.ty.createcraftedbeginning.api.coolantshandlers.contents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.CoolantEfficiency;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlueIceCoolantHandler implements AirtightCoolantHandler {
    @Override
    public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
        return CoolantEfficiency.ADVANCED;
    }

    @Override
    public @Nullable BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
        return Blocks.PACKED_ICE.defaultBlockState();
    }
}
