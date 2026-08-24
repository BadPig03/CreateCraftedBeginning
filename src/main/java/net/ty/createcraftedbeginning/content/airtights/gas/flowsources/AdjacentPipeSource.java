package net.ty.createcraftedbeginning.content.airtights.gas.flowsources;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AdjacentPipeSource extends GasFlowSource {
    private WeakReference<GasTransportBehaviour> cached;

    public AdjacentPipeSource(BlockFace location) {
        super(location);
    }

    @Override
    public boolean isEndpoint() {
        return false;
    }

    @Override
    public void manageSource(Level level, BlockEntity networkBE) {
        GasTransportBehaviour targetBehaviour;
        if (cached != null) {
            targetBehaviour = cached.get();
            if (targetBehaviour != null && !targetBehaviour.blockEntity.isRemoved()) {
                return;
            }
        }

        cached = null;
        BlockEntity targetBlockEntity = level.getBlockEntity(location.getConnectedPos());
        if (targetBlockEntity != null) {
            targetBehaviour = BlockEntityBehaviour.get(targetBlockEntity, GasTransportBehaviour.TYPE);
        }
        else {
            targetBehaviour = BlockEntityBehaviour.get(level, location.getConnectedPos(), GasTransportBehaviour.TYPE);
        }
        if (targetBehaviour == null) {
            return;
        }

        cached = new WeakReference<>(targetBehaviour);
    }

    @Override
    public GasStack provideGas(Predicate<GasStack> gasPredicate) {
        if (cached == null || cached.get() == null) {
            return GasStack.EMPTY;
        }

        GasTransportBehaviour targetBehaviour = cached.get();
        if (targetBehaviour == null) {
            return GasStack.EMPTY;
        }

        GasStack outwardGas = targetBehaviour.getProvidedOutwardGas(location.getOppositeFace());
        if (!gasPredicate.test(outwardGas)) {
            return GasStack.EMPTY;
        }
        return outwardGas;
    }
}