package net.ty.createcraftedbeginning.api.coolantshandlers;

import com.mojang.serialization.Codec;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CoolantEfficiency implements StringRepresentable {
    NONE(0),
    BASIC(2),
    ADVANCED(4),
    EXTREME(6);

    public static final Codec<CoolantEfficiency> CODEC = StringRepresentable.fromEnum(CoolantEfficiency::values);

    private final int heatReduced;

    CoolantEfficiency(int heatReduced) {
        this.heatReduced = heatReduced;
    }

    /**
     * Resolves a coolant efficiency from its ordinal-style index, clamping values outside the supported range.
     *
     * @param index the efficiency index to resolve
     * @return the resolved coolant efficiency
     */
    public static CoolantEfficiency fromInt(int index) {
        if (index <= 0) {
            return NONE;
        }

        CoolantEfficiency[] efficiencies = values();
        if (index >= efficiencies.length - 1) {
            return EXTREME;
        }
        return efficiencies[index];
    }

    /**
     * Resolves a coolant efficiency from its serialized name.
     *
     * @param name the serialized efficiency name
     * @return the matching efficiency, or {@link #NONE} when no value matches
     */
    public static CoolantEfficiency fromName(String name) {
        for (CoolantEfficiency efficiency : values()) {
            if (!efficiency.getSerializedName().equals(name)) {
                continue;
            }

            return efficiency;
        }
        return NONE;
    }

    /**
     * Returns the amount of heat reduced in the supplied level, including passive cooling when available.
     *
     * @param level the level whose dimension properties affect passive cooling
     * @return the effective heat reduction
     */
    public int getHeatReduced(Level level) {
        int passiveCooling = level.dimensionType().ultraWarm() ? 0 : 1;
        return Math.max(passiveCooling, heatReduced);
    }

    /**
     * Returns the serialized name used by codecs and data files.
     *
     * @return the serialized efficiency name
     */
    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
