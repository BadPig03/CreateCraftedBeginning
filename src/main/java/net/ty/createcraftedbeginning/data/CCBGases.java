package net.ty.createcraftedbeginning.data;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.GasHolder;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class CCBGases {
    public static final CCBGasDeferredRegister GAS_REGISTER = new CCBGasDeferredRegister(CreateCraftedBeginning.MOD_ID);

    public static final GasHolder<Gas, Gas> NATURAL_AIR = GAS_REGISTER.register("natural_air", builder().tint(0xFFCBCFFA).inflation(1).engineEfficiency(1).teslaEfficiency(1).tag(CCBGasTags.NATURAL.tag));
    public static final GasHolder<Gas, Gas> ULTRAWARM_AIR = GAS_REGISTER.register("ultrawarm_air", builder().tint(0xFFF0D2B2).inflation(1).engineEfficiency(1).teslaEfficiency(1).tag(CCBGasTags.ULTRAWARM.tag));
    public static final GasHolder<Gas, Gas> ETHEREAL_AIR = GAS_REGISTER.register("ethereal_air", builder().tint(0xFFF4BAF5).inflation(1).engineEfficiency(2).teslaEfficiency(2).tag(CCBGasTags.ETHEREAL.tag));

    public static final GasHolder<Gas, Gas> MOIST_AIR = GAS_REGISTER.register("moist_air", builder().tint(0xFFBEDAED).inflation(1).engineEfficiency(1).teslaEfficiency(1).tag(CCBGasTags.MOIST.tag));
    public static final GasHolder<Gas, Gas> SPORE_AIR = GAS_REGISTER.register("spore_air", builder().tint(0XFFE8F5C4).inflation(1).engineEfficiency(0).teslaEfficiency(0).tag(CCBGasTags.SPORE.tag));
    public static final GasHolder<Gas, Gas> SCULK_AIR = GAS_REGISTER.register("sculk_air", builder().tint(0xFF111B21).inflation(1).engineEfficiency(0).teslaEfficiency(0).tag(CCBGasTags.SCULK.tag));

    public static final GasHolder<Gas, Gas> ENERGIZED_NATURAL_AIR = GAS_REGISTER.register("energized_natural_air", builder().inflation(1).tint(0xFFD2D5FB).engineEfficiency(0).teslaEfficiency(2).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> ENERGIZED_ULTRAWARM_AIR = GAS_REGISTER.register("energized_ultrawarm_air", builder().inflation(1).tint(0xFFF2D8BC).engineEfficiency(0).teslaEfficiency(3).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> ENERGIZED_ETHEREAL_AIR = GAS_REGISTER.register("energized_ethereal_air", builder().inflation(1).tint(0xFFF5C3F6).engineEfficiency(0).teslaEfficiency(4).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.ENERGIZED.tag));

    public static final GasHolder<Gas, Gas> PRESSURIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_natural_air", builder().inflation(2).tint(0xFF999CBC).engineEfficiency(4).teslaEfficiency(4).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_ultrawarm_air", builder().inflation(2).tint(0xFFB59E86).engineEfficiency(6).teslaEfficiency(6).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_ethereal_air", builder().inflation(2).tint(0xFFB88CB8).engineEfficiency(8).teslaEfficiency(8).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.PRESSURIZED.tag));

    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_energized_natural_air", builder().inflation(2).tint(0xFF9EA0BD).engineEfficiency(0).teslaEfficiency(8).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag).tag(CCBGasTags.PRESSURIZED_ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_energized_ultrawarm_air", builder().inflation(2).tint(0xFFB6A38E).engineEfficiency(0).teslaEfficiency(12).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag).tag(CCBGasTags.PRESSURIZED_ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_energized_ethereal_air", builder().inflation(2).tint(0xFFB893B9).engineEfficiency(0).teslaEfficiency(16).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag).tag(CCBGasTags.PRESSURIZED_ENERGIZED.tag));

    public static final GasHolder<Gas, Gas> CREATIVE_AIR = GAS_REGISTER.register("creative_air", builder().inflation(2).tint(0xFF000000).inflation(2).engineEfficiency(16).teslaEfficiency(16).tag(CCBGasTags.CREATIVE.tag));

    @Contract(" -> new")
    private static @NotNull GasBuilder builder() {
        return GasBuilder.builder();
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        GAS_REGISTER.getEntries().stream().map(DeferredHolder::getId).forEach(id -> consumer.accept("gas." + id.getNamespace() + '.' + id.getPath(), formatGasName(id.getPath())));
    }

    private static @NotNull String formatGasName(@NotNull String registryName) {
        String[] words = registryName.replace('_', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1).toLowerCase());
            }
        }
        return builder.toString();
    }
}
