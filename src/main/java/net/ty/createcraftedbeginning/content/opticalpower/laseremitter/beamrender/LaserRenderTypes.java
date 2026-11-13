package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamBrightness;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class LaserRenderTypes {
    private static final String LASER_KEY = "createcraftedbeginning:laser";
    private static final ResourceLocation SHADER_ID = CCBAPI.asResource("laser_beam");
    static final RenderType LASER = createLaser(LASER_KEY, false);
    static final RenderType PONDER_LASER = createLaser("createcraftedbeginning:ponder_laser", true);

    private static @Nullable ShaderBinding binding;
    private static int shaderRevision;

    private LaserRenderTypes() {
    }

    static boolean isAvailable() {
        return binding != null;
    }

    static int shaderRevision() {
        return shaderRevision;
    }

    static ShaderInstance prepareLaserShader(boolean ponder) {
        ShaderBinding current = binding;
        if (current == null) {
            throw new IllegalStateException("Laser rendering requires the laser beam shader.");
        }

        current.fog().set(ponder ? 0 : 1);
        return current.shader();
    }

    @SubscribeEvent
    private static void registerShaders(RegisterShadersEvent event) {
        binding = null;
        shaderRevision++;
        ShaderInstance candidate;
        try {
            candidate = new ShaderInstance(event.getResourceProvider(), SHADER_ID, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        catch (IOException failure) {
            CCBAPI.LOGGER.warn("Could not load laser beam shader '{}'; disabling laser effects until the next resource reload.", SHADER_ID, failure);
            return;
        }

        event.registerShader(candidate, shader -> {
            Uniform minWidth = shader.getUniform("MinWidth");
            Uniform referenceRadius = shader.getUniform("ReferenceRadius");
            Uniform fog = shader.getUniform("FogEnabled");
            if (minWidth == null || minWidth.getType() != Uniform.UT_FLOAT1 || minWidth.getCount() != 1 || referenceRadius == null || referenceRadius.getType() != Uniform.UT_FLOAT1 || referenceRadius.getCount() != 1 || fog == null || fog.getType() != Uniform.UT_INT1 || fog.getCount() != 1) {
                CCBAPI.LOGGER.warn("Laser beam shader '{}' is missing a required scalar uniform or has an incompatible uniform type: MinWidth (float), ReferenceRadius (float), FogEnabled (int). Laser effects are disabled until the next resource reload.", SHADER_ID);
                return;
            }

            minWidth.set((float) BeamProfile.MIN_WIDTH);
            referenceRadius.set((float) LaserBeamBrightness.REFERENCE_RADIUS);
            binding = new ShaderBinding(shader, fog);
        });
    }

    private static RenderType createLaser(String name, boolean ponder) {
        CompositeState state = CompositeState.builder().setShaderState(new ShaderStateShard(() -> prepareLaserShader(ponder))).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setOutputState(ponder ? RenderStateShard.MAIN_TARGET : RenderStateShard.TRANSLUCENT_TARGET).createCompositeState(false);
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS, 256, false, false, state);
    }

    private record ShaderBinding(ShaderInstance shader, Uniform fog) {
    }
}
