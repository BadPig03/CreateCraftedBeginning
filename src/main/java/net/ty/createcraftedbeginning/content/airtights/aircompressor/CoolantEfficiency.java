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
    NONE,
    BASIC,
    ADVANCED,
    EXTREME;

    public static final Codec<CoolantEfficiency> CODEC = StringRepresentable.fromEnum(CoolantEfficiency::values);

    public static CoolantEfficiency fromInt(int index) {
        if (index <= 0) {
            return NONE;
        }
        else if (index >= 3) {
            return EXTREME;
        }
        return values()[index];
    }

    public int getHeatReduced(Level level) {
        int passive = level.dimensionType().ultraWarm() ? 0 : 1;
        return Math.max(passive, ordinal() * 2);
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
