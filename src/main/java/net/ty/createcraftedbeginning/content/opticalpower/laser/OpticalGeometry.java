package net.ty.createcraftedbeginning.content.opticalpower.laser;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface OpticalGeometry {
    VoxelShape getCollisionShape();

    OpticalSurface getSurface(BlockHitResult hit, Vec3 localDirection);

    default @Nullable List<Double> displayKey() {
        return null;
    }

    default VoxelShape getReceivingShape() {
        return Shapes.empty();
    }
}
