package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.AirFlowParticleAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasTags;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberUtils {
    private static final Map<ResourceLocation, Integer> COLORS = new HashMap<>();

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    static {
        COLORS.put(Create.asResource("splashing"), ARGB32.average(0xFF4499FF, 0xFF2277FF));
        COLORS.put(Create.asResource("smoking"), ARGB32.average(0xFF000000, 0xFF555555));
        COLORS.put(Create.asResource("blasting"), ARGB32.average(0xFFFF4400, 0xFFFF8855));
        COLORS.put(Create.asResource("haunting"), ARGB32.average(0xFF000000, 0xFF126568));
        COLORS.put(CCBAPI.asResource("chilling"), 0xFFEBF6FF);
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_white"), 0xFF000000 | DyeColor.WHITE.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_light_gray"), 0xFF000000 | DyeColor.LIGHT_GRAY.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_gray"), 0xFF000000 | DyeColor.GRAY.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_black"), 0xFF000000 | DyeColor.BLACK.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_brown"), 0xFF000000 | DyeColor.BROWN.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_red"), 0xFF000000 | DyeColor.RED.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_orange"), 0xFF000000 | DyeColor.ORANGE.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_yellow"), 0xFF000000 | DyeColor.YELLOW.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_lime"), 0xFF000000 | DyeColor.LIME.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_green"), 0xFF000000 | DyeColor.GREEN.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_cyan"), 0xFF000000 | DyeColor.CYAN.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_light_blue"), 0xFF000000 | DyeColor.LIGHT_BLUE.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_blue"), 0xFF000000 | DyeColor.BLUE.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_purple"), 0xFF000000 | DyeColor.PURPLE.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_magenta"), 0xFF000000 | DyeColor.MAGENTA.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "coloring_pink"), 0xFF000000 | DyeColor.PINK.getFireworkColor());
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "ending"), ARGB32.average(0xFFB700D2, 0xFFDF00F9));
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "sanding"), 0xFFDBD3A0);
        COLORS.put(ResourceLocation.fromNamespaceAndPath("create_dragons_plus", "freezing"), ARGB32.average(0xFFFFFFFF, 0xFF8ADCE8));
        COLORS.put(ResourceLocation.fromNamespaceAndPath("dndesires", "seething"), ARGB32.average(0xFF64C9FD, 0xFF3F74E8));
    }

    private GasInjectionChamberUtils() {
    }

    public static boolean isFilter(ItemStack stack) {
        return stack.is(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get());
    }

    private static int sampleColor(FanProcessingType processingType, RandomSource random) {
        ColorCapture colorCapture = new ColorCapture();
        processingType.morphAirFlow(colorCapture, random);
        if (!colorCapture.hasColor) {
            return DEFAULT_COLOR;
        }
        return 0xFF000000 | colorCapture.color;
    }

    public static Optional<ResourceLocation> getFanProcessingTypeId(ItemStack stack) {
        if (!isFilter(stack)) {
            return Optional.empty();
        }
        return Optional.ofNullable(stack.get(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE));
    }

    public static Optional<FanProcessingType> getFanProcessingType(ResourceLocation typeId) {
        return Optional.ofNullable(CreateBuiltInRegistries.FAN_PROCESSING_TYPE.get(typeId));
    }

    public static int getColor(ItemStack stack) {
        if (!isFilter(stack)) {
            return DEFAULT_COLOR;
        }
        return stack.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, DEFAULT_COLOR);
    }

    public static long getFanProcessingGasCost(GasStack gas, int itemCount) {
        if (gas.isEmpty() || itemCount <= 0 || GasTags.isTag(gas, CCBGasTags.CREATIVE.tag)) {
            return 0;
        }

        int efficiencyDivisor = 1;
        if (GasTags.isTag(gas, CCBGasTags.ENERGIZED.tag)) {
            efficiencyDivisor *= 5;
        }
        if (GasTags.isTag(gas, CCBGasTags.PRESSURIZED.tag)) {
            efficiencyDivisor *= 20;
        }
        long baseCost = (long) CCBConfig.server().airtights.baseFanProcessingGasPerItem.get() * itemCount;
        return baseCost / efficiencyDivisor + (baseCost % efficiencyDivisor == 0 ? 0 : 1);
    }

    public static int getMaxFanProcessingBatchSize(GasStack gas, int desiredCount, long gasBudget) {
        if (gas.isEmpty() || desiredCount <= 0) {
            return 0;
        }

        if (GasTags.isTag(gas, CCBGasTags.CREATIVE.tag) || !consumesFanProcessingGas(gas)) {
            return desiredCount;
        }

        if (gasBudget <= 0) {
            return 0;
        }

        int minimumBatchSize = 0;
        int maximumBatchSize = desiredCount;
        while (minimumBatchSize < maximumBatchSize) {
            int candidateBatchSize = minimumBatchSize + (maximumBatchSize - minimumBatchSize + 1) / 2;
            if (getFanProcessingGasCost(gas, candidateBatchSize) > gasBudget) {
                maximumBatchSize = candidateBatchSize - 1;
                continue;
            }

            minimumBatchSize = candidateBatchSize;
        }
        return minimumBatchSize;
    }

    public static boolean consumesFanProcessingGas(GasStack gas) {
        return CCBConfig.server().airtights.baseFanProcessingGasPerItem.get() > 0 && !gas.isEmpty() && !GasTags.isTag(gas, CCBGasTags.CREATIVE.tag);
    }

    public static ItemStack create(ItemStack input, FanProcessingType type) {
        ItemStack filterStack = input.copy();
        ResourceLocation typeId = CreateBuiltInRegistries.FAN_PROCESSING_TYPE.getKey(type);
        if (typeId == null) {
            filterStack.remove(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE);
            return filterStack;
        }

        filterStack.set(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_FAN_PROCESSING_TYPE, typeId);
        Integer presetColor = COLORS.get(typeId);
        int filterColor = presetColor != null ? presetColor : sampleColor(type, RandomSource.create(typeId.hashCode()));
        filterStack.set(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, filterColor);
        return filterStack;
    }

    private static String getFanProcessingTypeTranslationKey(ResourceLocation typeId) {
        return "fan_processing_type." + typeId.getNamespace() + '.' + typeId.getPath().replace('/', '.');
    }

    public static Component getFanProcessingTypeName(ResourceLocation typeId) {
        return Component.translatableWithFallback(getFanProcessingTypeTranslationKey(typeId), typeId.toString());
    }

    private static final class ColorCapture implements AirFlowParticleAccess {
        private int color = DEFAULT_COLOR;
        private boolean hasColor;

        @Override
        public void setColor(int color) {
            this.color = color;
            hasColor = true;
        }

        @Override
        public void setAlpha(float alpha) {
        }

        @Override
        public void spawnExtraParticle(ParticleOptions options, float speedMultiplier) {
        }
    }
}
