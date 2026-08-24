package net.ty.createcraftedbeginning.content.fluids.brimstone;

import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.fluids.SolidRenderedPlaceableFluidType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BrimstoneFluidType extends SolidRenderedPlaceableFluidType {
    private BrimstoneFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture, int fogColor, Supplier<Float> fogDistance) {
        super(properties, stillTexture, flowingTexture, new Color(fogColor, false).asVectorF(), fogDistance);
    }

    public static FluidTypeFactory create(int fogColor, Supplier<Float> fogDistance) {
        return (properties, stillTexture, flowingTexture) -> new BrimstoneFluidType(properties, stillTexture, flowingTexture, fogColor, fogDistance);
    }

    @Override
    public boolean move(FluidState state, LivingEntity entity, Vec3 movementVector, double gravity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.25, 0.75, 0.25));
        return false;
    }

    @Override
    public void setItemMovement(ItemEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double verticalSpeed = movement.y + (movement.y < 0.06 ? 5.0e-4 : 0);
        entity.setDeltaMovement(movement.x * 0.8, verticalSpeed, movement.z * 0.8);
    }
}
