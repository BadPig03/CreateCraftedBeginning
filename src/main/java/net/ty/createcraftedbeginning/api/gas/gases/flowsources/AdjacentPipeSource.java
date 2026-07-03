package net.ty.createcraftedbeginning.api.gas.gases.flowsources;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;

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
    public GasStack provideGas(Predicate<GasStack> predicate) {
        if (cached == null || cached.get() == null) {
            return GasStack.EMPTY;
        }

        GasTransportBehaviour behaviour = cached.get();
        if (behaviour == null) {
            return GasStack.EMPTY;
        }

        GasStack outwardGas = behaviour.getProvidedOutwardGas(location.getOppositeFace());
        if (!predicate.test(outwardGas)) {
            return GasStack.EMPTY;
        }

        return outwardGas;
    }

    @Override
    public void manageSource(Level level, BlockEntity networkBE) {
        GasTransportBehaviour behaviour;
        if (cached != null) {
            behaviour = cached.get();
            if (behaviour != null && !behaviour.blockEntity.isRemoved()) {
                return;
            }
        }

        cached = null;
        BlockEntity targetBE = level.getBlockEntity(location.getConnectedPos());
        if (targetBE != null) {
            behaviour = BlockEntityBehaviour.get(targetBE, GasTransportBehaviour.TYPE);
        }
        else {
            behaviour = BlockEntityBehaviour.get(level, location.getConnectedPos(), GasTransportBehaviour.TYPE);
        }
        if (behaviour == null) {
            return;
        }

        cached = new WeakReference<>(behaviour);
    }
}