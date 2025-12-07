package net.ty.createcraftedbeginning.api.outliner;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.createmod.ponder.enums.PonderSpecialTextures;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CCBRenderTypes {
    public static final RenderType SOLID_NO_DEPTH_TEST = createSolidNoDepthTest();

    private static final String NAME = "outline_solid_no_depth_test";

    @Contract(" -> new")
    private static @NotNull RenderType createSolidNoDepthTest() {
        return RenderType.create(NAME, DefaultVertexFormat.NEW_ENTITY, Mode.QUADS, 256, false, false, CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER).setTextureState(new TextureStateShard(PonderSpecialTextures.BLANK.getLocation(), false, false)).setCullState(RenderStateShard.NO_CULL).setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).createCompositeState(false));
    }
}
