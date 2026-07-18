package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonGasContents;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPackagerUtils {
    private GasPackagerUtils() {
    }

    public static boolean supportsGasHandler(@Nullable BlockEntity target) {
        return target != null && !(target instanceof PortableGasInterfaceBlockEntity);
    }

    public static boolean supportsItemHandler(@Nullable BlockEntity target) {
        return target != null && !(target instanceof PortableStorageInterfaceBlockEntity);
    }

    public static boolean isSameLink(PackagingRequest first, PackagingRequest second) {
        return first.orderId() == second.orderId() && first.linkIndex() == second.linkIndex() && first.address().equals(second.address());
    }

    public static boolean propagatePackageCounter(PackagingRequest completed, List<PackagingRequest> queue, int nextPackageIndex) {
        while (!queue.isEmpty() && isSameLink(completed, queue.getFirst())) {
            PackagingRequest next = queue.getFirst();
            if (next.getCount() <= 0 || !GasVirtualUtils.isVirtualItem(next.item()) || GasVirtualUtils.getGasType(next.item()).isEmpty()) {
                queue.removeFirst();
                continue;
            }

            next.packageCounter().setValue(nextPackageIndex);
            return false;
        }
        return true;
    }

    public static boolean matchesTankSnapshot(IGasHandler handler, List<GasStack> snapshot) {
        int tankCount = Math.max(0, handler.getTanks());
        if (snapshot.size() != tankCount) {
            return false;
        }

        for (int tank = 0; tank < tankCount; tank++) {
            if (!GasStack.matches(snapshot.get(tank), handler.getGasInTank(tank))) {
                return false;
            }
        }
        return true;
    }

    public static @Unmodifiable List<GasStack> snapshotTanks(IGasHandler handler) {
        int tankCount = Math.max(0, handler.getTanks());
        List<GasStack> snapshot = new ArrayList<>(tankCount);
        for (int tank = 0; tank < tankCount; tank++) {
            snapshot.add(handler.getGasInTank(tank).copy());
        }
        return List.copyOf(snapshot);
    }

    public static BalloonGasContents drainContents(IGasHandler handler, long maxAmount) {
        if (maxAmount <= 0) {
            return BalloonGasContents.EMPTY;
        }

        List<GasStack> drainedGases = new ArrayList<>();
        long remaining = maxAmount;
        int tankCount = Math.max(0, handler.getTanks());
        for (int tank = 0; tank < tankCount; tank++) {
            if (remaining <= 0 || drainedGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
                break;
            }

            GasStack gas = handler.getGasInTank(tank);
            if (gas.isEmpty()) {
                continue;
            }

            long amount = Math.min(remaining, gas.getAmount());
            GasStack requested = gas.copyWithAmount(amount);
            GasStack simulated = handler.drain(requested, GasAction.SIMULATE);
            if (simulated.isEmpty() || !GasStack.isSameGasSameComponents(simulated, requested)) {
                continue;
            }

            long drainAmount = Math.min(amount, simulated.getAmount());
            GasStack drained = handler.drain(simulated.copyWithAmount(drainAmount), GasAction.EXECUTE);
            if (drained.isEmpty() || !GasStack.isSameGasSameComponents(drained, requested)) {
                continue;
            }

            drainedGases.add(drained.copy());
            remaining -= drained.getAmount();
        }
        return new BalloonGasContents(drainedGases);
    }

    public static boolean canInsertAll(IGasHandler handler, BalloonGasContents contents) {
        List<GasStack> gases = contents.copyGasStacks();
        if (gases.size() > 1) {
            return handler.tryFillAtomically(gases, GasAction.SIMULATE).isSuccess();
        }

        List<SimulatedTank> tanks = new ArrayList<>(handler.getTanks());
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            tanks.add(new SimulatedTank(handler.getGasInTank(tank).copy(), Math.max(0, handler.getTankCapacity(tank))));
        }

        for (GasStack gas : gases) {
            if (handler.fill(gas.copy(), GasAction.SIMULATE) < gas.getAmount()) {
                return false;
            }

            long remaining = gas.getAmount();
            for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
                SimulatedTank tankState = tanks.get(tank);
                if (tankState.gas().isEmpty() || !GasStack.isSameGasSameComponents(tankState.gas(), gas) || !handler.isGasValid(tank, gas)) {
                    continue;
                }

                remaining -= tankState.fill(gas, remaining);
            }

            for (int tank = 0; tank < tanks.size() && remaining > 0; tank++) {
                SimulatedTank tankState = tanks.get(tank);
                if (!tankState.gas().isEmpty() || !handler.isGasValid(tank, gas)) {
                    continue;
                }

                remaining -= tankState.fill(gas, remaining);
            }

            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static final class SimulatedTank {
        private final long capacity;
        private GasStack gas;

        private SimulatedTank(GasStack gas, long capacity) {
            this.gas = gas.isEmpty() ? GasStack.EMPTY : gas.copy();
            this.capacity = capacity;
        }

        private GasStack gas() {
            return gas;
        }

        private long fill(GasStack resource, long requested) {
            long current = gas.getAmount();
            long accepted = Math.clamp(capacity - current, 0, requested);
            if (accepted <= 0) {
                return 0;
            }

            gas = resource.copyWithAmount(current + accepted);
            return accepted;
        }
    }
}
