package net.ty.createcraftedbeginning.api.coolantshandlers;

import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCoolantHandlerEvent.EfficiencyCoolantHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCoolantHandlerEvent.MeltCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.CoolantEfficiency;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Provides lookup and registration helpers for airtight coolant behavior.
 * Handlers define coolant efficiency and the block state produced when a coolant melts or is consumed.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCoolantHandlerUtils {
    private AirtightCoolantHandlerUtils() {
    }

    /**
     * Resolves the airtight coolant handler associated with the supplied input.
     *
     * @param block the target block
     * @return the resolved airtight coolant handler
     */
    public static AirtightCoolantHandler of(Block block) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler == null) {
            return DefaultCoolantHandler.INSTANCE;
        }
        return coolantHandler;
    }

    /**
     * Registers a custom airtight coolant handler for the supplied target.
     *
     * @param block      the target block
     * @param efficiency the efficiency to use
     * @param melt       the melt to use
     */
    public static void register(Block block, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandler.REGISTRY.register(block, new AirtightCoolantHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
                return CoolantEfficiency.fromInt(efficiency.apply(level, pos, blockState));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
                return BuiltInRegistries.BLOCK.getOptional(melt.apply(level, pos, blockState)).orElse(Blocks.AIR).defaultBlockState();
            }
        });
    }

    /**
     * Registers a custom airtight coolant handler for the supplied target.
     *
     * @param predicate  the predicate used to select matching values
     * @param efficiency the efficiency to use
     * @param melt       the melt to use
     */
    public static void register(BlockStatePredicate predicate, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? new AirtightCoolantHandler() {
            /**
             * {@inheritDoc}
             */
            @Override
            public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
                return CoolantEfficiency.fromInt(efficiency.apply(level, pos, blockState));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
                return BuiltInRegistries.BLOCK.getOptional(melt.apply(level, pos, blockState)).orElse(Blocks.AIR).defaultBlockState();
            }
        } : null);
    }

    /**
     * Registers a custom airtight coolant handler for the supplied target.
     *
     * @param block   the target block
     * @param handler the handler to register or invoke
     */
    public static void register(Block block, AirtightCoolantHandler handler) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler != null) {
            CreateCraftedBeginning.LOGGER.error("Failed to register Airtight Coolant Handler for block '{}': a handler is already registered.", block.kjs$getIdLocation());
            return;
        }

        AirtightCoolantHandler.REGISTRY.register(block, handler);
    }

    /**
     * Registers a custom airtight coolant handler for the supplied target.
     *
     * @param predicate the predicate used to select matching values
     * @param handler   the handler to register or invoke
     */
    public static void register(BlockStatePredicate predicate, AirtightCoolantHandler handler) {
        AirtightCoolantHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? handler : null);
    }
}
