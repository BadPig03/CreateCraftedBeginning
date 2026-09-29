package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.NumericSingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightManometerDisplaySource extends NumericSingleLineDisplaySource {
    private final Reading reading;

    public AirtightManometerDisplaySource(Reading reading) {
        this.reading = reading;
    }

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        if (!(context.getSourceBlockEntity() instanceof AirtightManometerBlockEntity manometer)) {
            return EMPTY_LINE;
        }

        if (!manometer.hasPressureReading()) {
            return Component.literal("--");
        }

        long pressure = switch (reading) {
            case MAX_PRESSURE -> manometer.getMaxPressurePa();
            case PRESSURE_DIFFERENCE -> manometer.getPressureDifferencePa();
        };
        MutableComponent component = Component.literal(GasPressure.formatAtm(pressure));
        if (reading == Reading.MAX_PRESSURE && GasPressureLimits.isOverpressure(pressure)) {
            component.withStyle(ChatFormatting.RED);
        }
        return component;
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }

    public enum Reading {
        MAX_PRESSURE,
        PRESSURE_DIFFERENCE
    }
}
