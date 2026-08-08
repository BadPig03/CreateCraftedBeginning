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
            if (remaining <= 0) {
                break;
            }

            GasStack gas = handler.getGasInTank(tank);
            if (gas.isEmpty()) {
                continue;
            }

            if (!containsMatchingGas(drainedGases, gas) && drainedGases.size() >= BalloonGasContents.MAX_GAS_TYPES) {
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

            addDrainedGas(drainedGases, drained);
            remaining -= drained.getAmount();
        }
        return new BalloonGasContents(drainedGases);
    }

    private static boolean containsMatchingGas(List<GasStack> gases, GasStack target) {
        return gases.stream().anyMatch(gas -> GasStack.isSameGasSameComponents(gas, target));
    }

    private static void addDrainedGas(List<GasStack> gases, GasStack added) {
        for (int i = 0; i < gases.size(); i++) {
            GasStack existing = gases.get(i);
            if (!GasStack.isSameGasSameComponents(existing, added)) {
                continue;
            }

            gases.set(i, existing.copyWithAmount(existing.getAmount() + added.getAmount()));
            return;
        }

        gases.add(added.copy());
    }

    public static boolean canInsertAll(IGasHandler handler, BalloonGasContents contents) {
        List<GasStack> gases = contents.copyGasStacks();
        if (gases.isEmpty()) {
            return true;
        }

        if (gases.size() > 1) {
            return handler.tryFillAtomically(gases, GasAction.SIMULATE).isSuccess();
        }

        GasStack gas = gases.getFirst();
        return handler.fill(gas.copy(), GasAction.SIMULATE) >= gas.getAmount();
    }
}
