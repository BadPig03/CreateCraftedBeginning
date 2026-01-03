package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasHolder;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class CCBGases {
    public static final CCBGasDeferredRegister GAS_REGISTER = new CCBGasDeferredRegister(CreateCraftedBeginning.MOD_ID);

    public static final GasHolder<Gas, Gas> NATURAL_AIR = GAS_REGISTER.register("natural_air", builder().tint(0xFFCBCFFA).pressurizedGas(CreateCraftedBeginning.asResource("pressurized_natural_air")).energizedGas(CreateCraftedBeginning.asResource("energized_natural_air")).engineEfficiency(1).teslaEfficiency(1).outputItemStack(() -> new ItemStack(Items.CLAY_BALL)).tag(CCBGasTags.NATURAL.tag));
    public static final GasHolder<Gas, Gas> ULTRAWARM_AIR = GAS_REGISTER.register("ultrawarm_air", builder().tint(0xFFF0D2B2).pressurizedGas(CreateCraftedBeginning.asResource("pressurized_ultrawarm_air")).energizedGas(CreateCraftedBeginning.asResource("energized_ultrawarm_air")).engineEfficiency(1.4f).teslaEfficiency(1.4f).outputItemStack(() -> new ItemStack(AllItems.CINDER_FLOUR.asItem())).tag(CCBGasTags.ULTRAWARM.tag));
    public static final GasHolder<Gas, Gas> ETHEREAL_AIR = GAS_REGISTER.register("ethereal_air", builder().tint(0xFFF4BAF5).pressurizedGas(CreateCraftedBeginning.asResource("pressurized_ethereal_air")).energizedGas(CreateCraftedBeginning.asResource("energized_ethereal_air")).engineEfficiency(2).teslaEfficiency(2).outputFluidStack(() -> new FluidStack(Fluids.WATER, 1)).tag(CCBGasTags.ETHEREAL.tag));
    public static final GasHolder<Gas, Gas> MOIST_AIR = GAS_REGISTER.register("moist_air", builder().tint(0xFFBEDAED).engineEfficiency(1).teslaEfficiency(1).outputFluidStack(() -> new FluidStack(Fluids.WATER, 1)).tag(CCBGasTags.MOIST.tag));
    public static final GasHolder<Gas, Gas> SPORE_AIR = GAS_REGISTER.register("spore_air", builder().tint(0XFFE8F5C4).engineEfficiency(1.8f).teslaEfficiency(1.8f).tag(CCBGasTags.SPORE.tag));

    public static final GasHolder<Gas, Gas> ENERGIZED_NATURAL_AIR = GAS_REGISTER.register("energized_natural_air", builder().tint(0xFFFFFFFF).pressurizedGas(CreateCraftedBeginning.asResource("pressurized_energized_natural_air")).engineEfficiency(0).teslaEfficiency(2).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> ENERGIZED_ULTRAWARM_AIR = GAS_REGISTER.register("energized_ultrawarm_air", builder().tint(0xFF622626).pressurizedGas(CreateCraftedBeginning.asResource("pressurized_energized_ultrawarm_air")).engineEfficiency(0).teslaEfficiency(2.8f).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.ENERGIZED.tag));
    public static final GasHolder<Gas, Gas> ENERGIZED_ETHEREAL_AIR = GAS_REGISTER.register("energized_ethereal_air", builder().tint(0xFF8D658D).pressurizedGas(CreateCraftedBeginning.asResource("pressurized_energized_ethereal_air")).engineEfficiency(0).teslaEfficiency(4).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.ENERGIZED.tag));

    public static final GasHolder<Gas, Gas> PRESSURIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_natural_air", builder().tint(0xFFFFFFFF).energizedGas(CreateCraftedBeginning.asResource("pressurized_energized_natural_air")).engineEfficiency(4).teslaEfficiency(4).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_ultrawarm_air", builder().tint(0xFF622626).energizedGas(CreateCraftedBeginning.asResource("pressurized_energized_ultrawarm_air")).engineEfficiency(6).teslaEfficiency(6).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_ethereal_air", builder().tint(0xFF8D658D).energizedGas(CreateCraftedBeginning.asResource("pressurized_energized_ethereal_air")).engineEfficiency(8).teslaEfficiency(8).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.PRESSURIZED.tag));

    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_energized_natural_air", builder().tint(0xFFFFFFFF).engineEfficiency(0).teslaEfficiency(8).tag(CCBGasTags.NATURAL.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_energized_ultrawarm_air", builder().tint(0xFF622626).engineEfficiency(0).teslaEfficiency(12).tag(CCBGasTags.ULTRAWARM.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag));
    public static final GasHolder<Gas, Gas> PRESSURIZED_ENERGIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_energized_ethereal_air", builder().tint(0xFF8D658D).engineEfficiency(0).teslaEfficiency(16).tag(CCBGasTags.ETHEREAL.tag).tag(CCBGasTags.ENERGIZED.tag).tag(CCBGasTags.PRESSURIZED.tag));

    public static final GasHolder<Gas, Gas> CREATIVE_AIR = GAS_REGISTER.register("creative_air", builder().tint(0xFF000000).engineEfficiency(16).teslaEfficiency(16).tag(CCBGasTags.CREATIVE.tag));

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
