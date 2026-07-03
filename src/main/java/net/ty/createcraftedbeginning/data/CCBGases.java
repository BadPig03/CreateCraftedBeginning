package net.ty.createcraftedbeginning.data;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.GasHolder;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBGases {
    public static final CCBGasDeferredRegister GAS_REGISTER = new CCBGasDeferredRegister(CreateCraftedBeginning.MOD_ID);

    public static final GasHolder<Gas, Gas> NATURAL_AIR = GAS_REGISTER.register("natural_air", builder().tint(0xCBCFFA).tag(CCBGasTags.NATURAL.tag));
    public static final GasHolder<Gas, Gas> ULTRAWARM_AIR = GAS_REGISTER.register("ultrawarm_air", builder().tint(0xF0D2B2).tag(CCBGasTags.ULTRAWARM.tag));
    public static final GasHolder<Gas, Gas> ETHEREAL_AIR = GAS_REGISTER.register("ethereal_air", builder().tint(0xF4BAF5).tag(CCBGasTags.ETHEREAL.tag));

    public static final GasHolder<Gas, Gas> ENERGIZED_NATURAL_AIR = GAS_REGISTER.register("energized_natural_air", builder().tint(0xD2D5FB).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> ENERGIZED_ULTRAWARM_AIR = GAS_REGISTER.register("energized_ultrawarm_air", builder().tint(0xF2D8BC).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> ENERGIZED_ETHEREAL_AIR = GAS_REGISTER.register("energized_ethereal_air", builder().tint(0xF5C3F6).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.ENERGIZED.tag));

    public static final GasHolder<Gas, Gas> PRESSURIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_natural_air", builder().tint(0x999CBC).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_ultrawarm_air", builder().tint(0xB59E86).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_ethereal_air", builder().tint(0xB88CB8).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.PRESSURIZED.tag));

    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_energized_natural_air", builder().tint(0x9EA0BD).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag).tag(CCBGasTags.PRESSURIZED_ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_energized_ultrawarm_air", builder().tint(0xB6A38E).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag).tag(CCBGasTags.PRESSURIZED_ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_energized_ethereal_air", builder().tint(0xB893B9).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag).tag(CCBGasTags.PRESSURIZED_ENERGIZED.tag));

    public static final GasHolder<Gas, Gas> MOIST_AIR = GAS_REGISTER.register("moist_air", builder().tint(0xBEDAED).tag(CCBGasTags.MOIST.tag));
    public static final GasHolder<Gas, Gas> SPORE_AIR = GAS_REGISTER.register("spore_air", builder().tint(0XE8F5C4).tag(CCBGasTags.SPORE.tag));
    public static final GasHolder<Gas, Gas> SCULK_AIR = GAS_REGISTER.register("sculk_air", builder().tint(0x111B21).tag(CCBGasTags.SCULK.tag));

    public static final GasHolder<Gas, Gas> CREATIVE_AIR = GAS_REGISTER.register("creative_air", builder().tint(0x000000).tag(CCBGasTags.CREATIVE.tag));

    @Contract(" -> new")
    private static GasBuilder builder() {
        return GasBuilder.builder();
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        GAS_REGISTER.getEntries().stream().map(DeferredHolder::getId).forEach(id -> consumer.accept("gas." + id.getNamespace() + '.' + id.getPath(), formatGasName(id.getPath())));
    }

    private static String formatGasName(String registryName) {
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
