package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowCollisionContext extends EntityCollisionContext {
    private final BlockPos sourcePos;

    GasFlowCollisionContext(Entity entity, BlockPos sourcePos) {
        super(entity.isDescending(), entity.getY(), entity instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY, entity instanceof LivingEntity living ? fluid -> living.canStandOnFluid(fluid) : fluid -> false, entity);
        this.sourcePos = sourcePos.immutable();
    }

    public boolean isSource(BlockPos pos) {
        return sourcePos.equals(pos);
    }
}
