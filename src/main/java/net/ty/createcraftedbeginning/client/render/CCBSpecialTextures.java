package net.ty.createcraftedbeginning.client.render;

import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.render.BindableTexture;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBSpecialTextures implements BindableTexture {
    LOW_TRANSLUCENT,
    LOW_TRANSLUCENT_HIGHLIGHTED;

    private final ResourceLocation location;

    CCBSpecialTextures() {
        location = CCBAPI.asResource("textures/special/" + Lang.asId(name()) + ".png");
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }
}
