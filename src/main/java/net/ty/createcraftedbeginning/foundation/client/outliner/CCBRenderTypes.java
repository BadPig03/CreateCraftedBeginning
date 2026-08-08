package net.ty.createcraftedbeginning.foundation.client.outliner;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.createmod.ponder.enums.PonderSpecialTextures;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class CCBRenderTypes {
    private static final String RENDER_TYPE_NAME = "outline_solid_no_depth_test";
    public static final RenderType SOLID_NO_DEPTH_TEST = createSolidNoDepthTest();

    @Contract(" -> new")
    private static RenderType createSolidNoDepthTest() {
        CompositeState state = CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER).setTextureState(new TextureStateShard(PonderSpecialTextures.BLANK.getLocation(), false, false)).setCullState(RenderStateShard.NO_CULL).setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).createCompositeState(false);
        return RenderType.create(RENDER_TYPE_NAME, DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, false, false, state);
    }
}
