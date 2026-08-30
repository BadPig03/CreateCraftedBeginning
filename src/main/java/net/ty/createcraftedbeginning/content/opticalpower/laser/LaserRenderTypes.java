package net.ty.createcraftedbeginning.content.opticalpower.laser;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class LaserRenderTypes {
    private static final String LASER_KEY = "createcraftedbeginning:laser";
    public static final RenderType LASER = createLaser();

    private LaserRenderTypes() {
    }

    private static RenderType createLaser() {
        CompositeState state = CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setCullState(RenderStateShard.NO_CULL).setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST).setWriteMaskState(RenderStateShard.COLOR_WRITE).setOutputState(RenderStateShard.TRANSLUCENT_TARGET).createCompositeState(false);
        return RenderType.create(LASER_KEY, DefaultVertexFormat.POSITION_COLOR, Mode.QUADS, 256, false, true, state);
    }
}
