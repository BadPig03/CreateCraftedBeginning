package net.ty.createcraftedbeginning.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasBuilder;
import net.ty.createcraftedbeginning.api.gas.GasType;
import net.ty.createcraftedbeginning.data.CCBGasDeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class CCBGases {
    public static final CCBGasDeferredRegister GAS_REGISTER = new CCBGasDeferredRegister(CreateCraftedBeginning.MOD_ID);

    //public static final GasType<Gas> BREEZE_WIND = GAS_REGISTER.register("breeze_wind", 0x408388B3, null, null);
    public static final GasType<Gas> ETHEREAL_AIR = GAS_REGISTER.register("ethereal_air", GasBuilder.builder().tint(0x408D658D).pressurizedGas("pressurized_ethereal_air").vortexedGas("ethereal_air_vortices"));
    public static final GasType<Gas> NATURAL_AIR = GAS_REGISTER.register("natural_air", GasBuilder.builder().tint(0x40FFFFFF).pressurizedGas("pressurized_natural_air").vortexedGas("natural_air_vortices"));
    public static final GasType<Gas> ULTRAWARM_AIR = GAS_REGISTER.register("ultrawarm_air", GasBuilder.builder().tint(0x40622626).pressurizedGas("pressurized_ultrawarm_air").vortexedGas("ultrawarm_air_vortices"));

    public static final GasType<Gas> ETHEREAL_AIR_VORTICES = GAS_REGISTER.register("ethereal_air_vortices", GasBuilder.builder().tint(0x408D658D).energy(3f));
    public static final GasType<Gas> NATURAL_AIR_VORTICES = GAS_REGISTER.register("natural_air_vortices", GasBuilder.builder().tint(0x40FFFFFF).energy(4f).condensate(new FluidStack(Fluids.WATER, 1)));
    public static final GasType<Gas> ULTRAWARM_AIR_VORTICES = GAS_REGISTER.register("ultrawarm_air_vortices", GasBuilder.builder().tint(0x40622626).energy(6f));

    public static final GasType<Gas> PRESSURIZED_ETHEREAL_AIR = GAS_REGISTER.register("pressurized_ethereal_air", GasBuilder.builder().tint(0x808D658D).depressurizedGas("ethereal_air").pressure(12f));
    public static final GasType<Gas> PRESSURIZED_NATURAL_AIR = GAS_REGISTER.register("pressurized_natural_air", GasBuilder.builder().tint(0x80FFFFFF).depressurizedGas("natural_air").pressure(12f));
    public static final GasType<Gas> PRESSURIZED_ULTRAWARM_AIR = GAS_REGISTER.register("pressurized_ultrawarm_air", GasBuilder.builder().tint(0x80622626).depressurizedGas("ultrawarm_air").pressure(12f));

    public static void provideLang(BiConsumer<String, String> consumer) {
        for (DeferredHolder<Gas, ? extends Gas> holder : GAS_REGISTER.getEntries()) {
            ResourceLocation id = holder.getId();
            String key = "gas." + id.getNamespace() + "." + id.getPath();
            String name = formatGasName(id.getPath());
            consumer.accept(key, name);
        }
    }

    private static @NotNull String formatGasName(@NotNull String registryName) {
        String spaced = registryName.replace('_', ' ');
        String[] words = spaced.split("\\s+");
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
