package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum PhotothermalChamberGeometry implements OpticalGeometry {
    INSTANCE;

    private static final VoxelShape RECEIVING_SHAPE = createReceivingShape();

    private static VoxelShape createReceivingShape() {
        VoxelShape shape = Shapes.empty();
        for (PhotothermalChamberPort port : PhotothermalChamberPort.getPorts()) {
            shape = Shapes.or(shape, port.getReceivingShape());
        }
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape() {
        return PhotothermalChamberPort.getLaserShape();
    }

    @Override
    public OpticalSurface getSurface(BlockHitResult hit, Vec3 localDirection) {
        return OpticalSurface.opaque(hit);
    }

    @Override
    public List<Double> displayKey() {
        return List.of();
    }

    @Override
    public VoxelShape getReceivingShape() {
        return RECEIVING_SHAPE;
    }
}
