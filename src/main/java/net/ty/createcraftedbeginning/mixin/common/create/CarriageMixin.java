package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Carriage.class)
abstract class CarriageMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "pivoted", at = @At("RETURN"), cancellable = true)
    private void ccb$pivoted(DimensionalCarriageEntity dimensional, ResourceKey<Level> dimension, TravellingPoint start, double offset, boolean leadingUpsideDown, boolean trailingUpsideDown, CallbackInfoReturnable<Vec3> callback) {
        Vec3 result = callback.getReturnValue();
        if (result == null || Double.isFinite(result.x) && Double.isFinite(result.y) && Double.isFinite(result.z)) {
            return;
        }

        Carriage carriage = (Carriage) (Object) this;
        if (carriage.train.graph == null || dimensional.pivot == null) {
            return;
        }

        boolean leading = start == carriage.getLeadingPoint();
        boolean flipped = !leading && leadingUpsideDown != trailingUpsideDown;
        Vec3 startPosition = start.getPosition(carriage.train.graph, flipped);
        Vec3 portalPosition = dimensional.pivot.getLocation().add(0, leadingUpsideDown ? -1 : 1, 0);
        if (startPosition.distanceToSqr(portalPosition) != 0) {
            return;
        }

        Vec3 direction = start.edge.getDirectionAt(start.position);
        double signedOffset = offset;
        if (leading) {
            signedOffset = -offset;
        }
        callback.setReturnValue(startPosition.add(direction.scale(signedOffset)));
    }
}
