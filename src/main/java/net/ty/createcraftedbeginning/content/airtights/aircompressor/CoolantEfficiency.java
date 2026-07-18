package net.ty.createcraftedbeginning.content.airtights.aircompressor;

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

    public static CoolantEfficiency fromName(String name) {
        for (CoolantEfficiency efficiency : values()) {
            if (!efficiency.getSerializedName().equals(name)) {
                continue;
            }

            return efficiency;
        }
        return NONE;
    }

    public int getHeatReduced(Level level) {
        int passiveCooling = level.dimensionType().ultraWarm() ? 0 : 1;
        return Math.max(passiveCooling, heatReduced);
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
