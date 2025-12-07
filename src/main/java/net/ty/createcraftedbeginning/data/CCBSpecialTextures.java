package net.ty.createcraftedbeginning.data;

import net.createmod.catnip.render.BindableTexture;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.NotNull;

public enum CCBSpecialTextures implements BindableTexture {
    LOW_TRANSLUCENT("low_translucent.png"),
    LOW_TRANSLUCENT_HIGHLIGHTED("low_translucent_highlighted.png");

    private final ResourceLocation location;

    CCBSpecialTextures(String filename) {
        location = CreateCraftedBeginning.asResource("textures/special/" + filename);
    }

    @Override
    public @NotNull ResourceLocation getLocation() {
        return location;
    }
}
