package net.ty.createcraftedbeginning.data;

import net.createmod.catnip.render.BindableTexture;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum CCBSpecialTextures implements BindableTexture {
    LOW_TRANSLUCENT("low_translucent.png"),
    LOW_TRANSLUCENT_HIGHLIGHTED("low_translucent_highlighted.png");

    private final ResourceLocation location;

    CCBSpecialTextures(String filename) {
        location = CreateCraftedBeginning.asResource("textures/special/" + filename);
    }

    @Override
    public ResourceLocation getLocation() {
        return location;
    }
}
