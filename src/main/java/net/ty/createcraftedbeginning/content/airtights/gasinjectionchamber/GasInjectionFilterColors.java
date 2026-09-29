package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType.AirFlowParticleAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.RandomSource;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionFilterColors {
    private static final Map<ResourceLocation, Integer> COLORS = new HashMap<>();

    static final int DEFAULT_COLOR = 0xFFFFFFFF;

    static {
        COLORS.put(Create.asResource("splashing"), ARGB32.average(0xFF4499FF, 0xFF2277FF));
        COLORS.put(Create.asResource("smoking"), ARGB32.average(0xFF000000, 0xFF555555));
        COLORS.put(Create.asResource("blasting"), ARGB32.average(0xFFFF4400, 0xFFFF8855));
        COLORS.put(Create.asResource("haunting"), ARGB32.average(0xFF000000, 0xFF126568));
        COLORS.put(CCBAPI.asResource("chilling"), 0xFFEBF6FF);
    }

    private GasInjectionFilterColors() {
    }

    public static void registerFanProcessingColor(ResourceLocation typeId, int color) {
        COLORS.put(typeId, color);
    }

    static int getColor(ResourceLocation typeId, FanProcessingType processingType) {
        Integer preset = COLORS.get(typeId);
        if (preset != null) {
            return preset;
        }

        RandomSource random = RandomSource.create(typeId.hashCode());
        ColorCapture colorCapture = new ColorCapture();
        processingType.morphAirFlow(colorCapture, random);
        if (!colorCapture.hasColor) {
            return DEFAULT_COLOR;
        }

        return 0xFF000000 | colorCapture.color;
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
