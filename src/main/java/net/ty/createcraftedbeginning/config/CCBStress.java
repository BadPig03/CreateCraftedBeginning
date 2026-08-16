package net.ty.createcraftedbeginning.config;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBStress extends ConfigBase {
    private static final Object2DoubleMap<ResourceLocation> DEFAULT_IMPACTS = new Object2DoubleOpenHashMap<>();
    private static final Object2DoubleMap<ResourceLocation> DEFAULT_CAPACITIES = new Object2DoubleOpenHashMap<>();

    protected final Map<ResourceLocation, ConfigValue<Double>> capacities = new HashMap<>();
    protected final Map<ResourceLocation, ConfigValue<Double>> impacts = new HashMap<>();

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
        return registerDefault(DEFAULT_IMPACTS, value);
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> setCapacity(double value) {
        return registerDefault(DEFAULT_CAPACITIES, value);
    }

    private static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> registerDefault(Object2DoubleMap<ResourceLocation> defaults, double value) {
        return builder -> {
            assertFromCreateCraftedBeginning(builder);
            ResourceLocation id = CCBAPI.asResource(builder.getName());
            defaults.put(id, value);
            return builder;
        };
    }

    private static void assertFromCreateCraftedBeginning(BlockBuilder<?, ?> builder) {
        if (builder.getOwner().getModid().equals(CCBAPI.MOD_ID)) {
            return;
        }

        throw new IllegalStateException("Blocks from other mods cannot be added to Create: Crafted Beginning's stress configuration.");
    }

    private static void registerValues(Builder builder, String name, String comment, Object2DoubleMap<ResourceLocation> defaults, Map<ResourceLocation, ConfigValue<Double>> values) {
        builder.comment(".", Comments.su, comment).push(name);
        defaults.forEach((id, value) -> values.put(id, builder.define(id.getPath(), value)));
        builder.pop();
    }

    private static @Nullable DoubleSupplier getValue(Block block, Map<ResourceLocation, ConfigValue<Double>> values) {
        ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(block);
        ConfigValue<Double> value = values.get(id);
        return value == null ? null : value::get;
    }

    @Override
    public void registerAll(Builder builder) {
        registerValues(builder, "impact", Comments.impact, DEFAULT_IMPACTS, impacts);
        registerValues(builder, "capacity", Comments.capacity, DEFAULT_CAPACITIES, capacities);
    }

    @Override
    public String getName() {
        return "stressValues";
    }

    @Nullable
    public DoubleSupplier getImpact(Block block) {
        return getValue(block, impacts);
    }

    @Nullable
    public DoubleSupplier getCapacity(Block block) {
        return getValue(block, capacities);
    }

    protected static class Comments {
        private static final String su = "[in Stress Units]";

        private static final String impact = "Configure the stress impact of individual mechanical blocks. Stress impact scales proportionally with rotational speed.";
        private static final String capacity = "Configure the stress capacity of individual kinetic sources.";
    }
}
