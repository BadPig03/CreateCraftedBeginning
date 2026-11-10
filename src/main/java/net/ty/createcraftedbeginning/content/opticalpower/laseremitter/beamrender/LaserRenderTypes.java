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
    static final RenderType LASER = createLaser(LASER_KEY, false);
    static final RenderType PONDER_LASER = createLaser("createcraftedbeginning:ponder_laser", true);

    private static @Nullable ShaderInstance laserShader;
    private static @Nullable Uniform fogEnabled;

    private LaserRenderTypes() {
    }

    static ShaderInstance prepareLaserShader(boolean ponder) {
        ShaderInstance shader = laserShader;
        Uniform fog = fogEnabled;
        if (shader == null || fog == null) {
            throw new IllegalStateException("Laser rendering requires the laser beam shader.");
        }

        fog.set(ponder ? 0 : 1);
        return shader;
    }

    @SubscribeEvent
    private static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), CCBAPI.asResource("laser_beam"), DefaultVertexFormat.POSITION_TEX_COLOR), shader -> {
            Uniform minWidth = shader.getUniform("MinWidth");
            Uniform referenceRadius = shader.getUniform("ReferenceRadius");
            Uniform fog = shader.getUniform("FogEnabled");
            if (minWidth == null || referenceRadius == null || fog == null) {
                throw new IllegalStateException("Laser beam shader requires MinWidth, ReferenceRadius and FogEnabled uniforms.");
            }

            minWidth.set((float) BeamProfile.MIN_WIDTH);
            referenceRadius.set((float) LaserBeamBrightness.REFERENCE_RADIUS);
            laserShader = shader;
            fogEnabled = fog;
        });
    }

    private static RenderType createLaser(String name, boolean ponder) {
        CompositeState state = CompositeState.builder().setShaderState(new ShaderStateShard(() -> prepareLaserShader(ponder))).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setOutputState(ponder ? RenderStateShard.MAIN_TARGET : RenderStateShard.TRANSLUCENT_TARGET).createCompositeState(false);
        return RenderType.create(name, DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS, 256, false, false, state);
    }
}
