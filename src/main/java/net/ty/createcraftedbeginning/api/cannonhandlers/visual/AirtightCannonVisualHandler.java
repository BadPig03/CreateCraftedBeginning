package net.ty.createcraftedbeginning.api.cannonhandlers.visual;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightCannonVisualHandler {
    ItemStack getRenderIcon(Level level);

    void renderTrailParticles(Level level, Vec3 pos, Vec3 velocity);

    ResourceLocation getTextureLocation();

    CannonModelType getModelType();

    CannonAnimationType getAnimationType();

    float getRotationSpeed();
}
