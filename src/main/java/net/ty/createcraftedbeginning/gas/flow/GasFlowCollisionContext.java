package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowCollisionContext extends EntityCollisionContext {
    private final BlockPos sourcePos;

    GasFlowCollisionContext(Entity entity, BlockPos sourcePos) {
        super(entity);
        this.sourcePos = sourcePos.immutable();
    }

    public boolean isSource(BlockPos pos) {
        return sourcePos.equals(pos);
    }
}
