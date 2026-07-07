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
 * Utility class for resolving and registering Airtight Coolant Handlers.
 * <p>
 * Coolant handlers are used to determine how efficiently a block cools an
 * airtight air compressor, and which block state the coolant should become after
 * melting or being consumed.
 * <p>
 * This class provides both Java-side registration methods using
 * {@link AirtightCoolantHandler} directly and KubeJS-friendly registration
 * methods using {@link EfficiencyCoolantHandler} and {@link MeltCoolantHandler}.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCoolantHandlerUtils {
    private AirtightCoolantHandlerUtils() {
    }

    /**
     * Gets the Airtight Coolant Handler registered for the given block.
     * <p>
     * If no handler is registered for the block, a {@link DefaultCoolantHandler}
     * instance is returned.
     *
     * @param block the block to get the coolant handler for
     * @return the registered coolant handler, or the default coolant handler if none is registered
     */
    public static AirtightCoolantHandler of(Block block) {
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandler.REGISTRY.get(block);
        if (coolantHandler == null) {
            return new DefaultCoolantHandler();
        }
        return coolantHandler;
    }

    /**
     * Registers a KubeJS coolant handler for the given block.
     * <p>
     * The provided {@link EfficiencyCoolantHandler} returns an integer coolant
     * efficiency value, which is converted through {@link CoolantEfficiency#fromInt(int)}.
     * The provided {@link MeltCoolantHandler} returns the resource location of the
     * block that should replace the coolant after melting.
     * <p>
     * If the returned melt block location does not point to a registered block,
     * {@link Blocks#AIR} is used instead.
     *
     * @param block      the block to register the coolant handler for
     * @param efficiency the KubeJS efficiency handler to register
     * @param melt       the KubeJS melt handler to register
     */
    public static void register(Block block, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandler.REGISTRY.register(block, new AirtightCoolantHandler() {
            @Override
            public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
                return CoolantEfficiency.fromInt(efficiency.apply(level, pos, blockState));
            }

            @Override
            public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
                return BuiltInRegistries.BLOCK.getOptional(melt.apply(level, pos, blockState)).orElse(Blocks.AIR).defaultBlockState();
            }
        });
    }

    /**
     * Registers a KubeJS coolant handler for blocks matching the given predicate.
     * <p>
     * The provided {@link EfficiencyCoolantHandler} returns an integer coolant
     * efficiency value, which is converted through {@link CoolantEfficiency#fromInt(int)}.
     * The provided {@link MeltCoolantHandler} returns the resource location of the
     * block that should replace matching coolant blocks after melting.
     * <p>
     * If the returned melt block location does not point to a registered block,
     * {@link Blocks#AIR} is used instead.
     *
     * @param predicate  the block state predicate used to select matching blocks
     * @param efficiency the KubeJS efficiency handler to register
     * @param melt       the KubeJS melt handler to register
     */
    public static void register(BlockStatePredicate predicate, EfficiencyCoolantHandler efficiency, MeltCoolantHandler melt) {
        AirtightCoolantHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? new AirtightCoolantHandler() {
            @Override
            public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
                return CoolantEfficiency.fromInt(efficiency.apply(level, pos, blockState));
            }

            @Override
            public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
                return BuiltInRegistries.BLOCK.getOptional(melt.apply(level, pos, blockState)).orElse(Blocks.AIR).defaultBlockState();
            }
        } : null);
    }

    /**
     * Registers an Airtight Coolant Handler for the given block.
     * <p>
     * If a handler is already registered for the block, registration is skipped and
     * an error is written to the mod logger.
     *
     * @param block   the block to register the coolant handler for
     * @param handler the airtight coolant handler to register
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
     * Registers an Airtight Coolant Handler provider for blocks matching the given
     * predicate.
     * <p>
     * The registered provider returns the supplied handler when the predicate
     * matches a block, otherwise it returns {@code null} so that other handlers or
     * fallback behavior may be used.
     *
     * @param predicate the block state predicate used to select matching blocks
     * @param handler   the airtight coolant handler to register
     */
    public static void register(BlockStatePredicate predicate, AirtightCoolantHandler handler) {
        AirtightCoolantHandler.REGISTRY.registerProvider(b -> predicate.testBlock(b) ? handler : null);
    }
}
