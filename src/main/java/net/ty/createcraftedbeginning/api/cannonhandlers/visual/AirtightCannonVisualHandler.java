package net.ty.createcraftedbeginning.api.cannonhandlers.visual;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightCannonVisualHandler {
    SimpleRegistry<Gas, AirtightCannonVisualHandler> REGISTRY = SimpleRegistry.create();

    ItemStack getRenderIcon(Level level);

    void renderTrailParticles(Level level, Vec3 pos);

    ResourceLocation getTextureLocation();

    CannonModelType getModelType();

    CannonAnimationType getAnimationType();

    float getRotationSpeed();
}
