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

    /**
     * Returns the render icon.
     *
     * @param level the level in which the operation is performed
     * @return the render icon
     */
    ItemStack getRenderIcon(Level level);

    /**
     * Renders the trail particles for the current cannon shot.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     */
    void renderTrailParticles(Level level, Vec3 pos);

    /**
     * Returns the texture location.
     *
     * @return the texture location
     */
    ResourceLocation getTextureLocation();

    /**
     * Returns the model type.
     *
     * @return the model type
     */
    CannonModelType getModelType();

    /**
     * Returns the animation type.
     *
     * @return the animation type
     */
    CannonAnimationType getAnimationType();

    /**
     * Returns the rotation speed.
     *
     * @return the rotation speed
     */
    float getRotationSpeed();
}
