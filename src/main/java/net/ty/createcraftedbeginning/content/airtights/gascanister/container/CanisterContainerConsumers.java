package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.api.gascanisters.IGasCanisterContainer;
import net.ty.createcraftedbeginning.api.gascanisters.events.CanisterContainerEvent;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenuSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerConsumers {
    private static final String COMPOUND_KEY_CANISTER = "Canister";

    private CanisterContainerConsumers() {
    }

    public static boolean interactContainer(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier, boolean simulate) {
        if (player.isCreative() || gasType.isEmpty() || amount <= 0) {
            return true;
        }

        if (player.level().isClientSide && !simulate) {
            return true;
        }

        List<IGasCanisterContainer> containers = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containers.isEmpty()) {
            return false;
        }

        if (player.level().isClientSide) {
            return canCoverCost(containers, gasType, amount, player);
        }

        if (!executeSupplier.get()) {
            return true;
        }

        CanisterContainerEvent event = new CanisterContainerEvent(player, gasType, amount, executeSupplier, simulate);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return true;
        }

        long adjustedAmount = event.getAmount();
        if (adjustedAmount <= 0) {
            return true;
        }

        if (!drainContainer(containers, gasType, adjustedAmount, simulate, player)) {
            return false;
        }

        if (simulate || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        CatnipServices.NETWORK.sendToClient(serverPlayer, new CanisterContainerClientPacket(new GasStack(gasType, adjustedAmount)));
        return true;
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player, ToDoubleFunction<Gas> rawCostFunction) {
        Set<Gas> checkedGases = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IGasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack content = container.getGasInTank(tank);
                if (content.isEmpty()) {
                    continue;
                }

                Gas gas = content.getGasType();
                if (!checkedGases.add(gas)) {
                    continue;
                }

                long amount = GasConsumptions.roundUp(rawCostFunction.applyAsDouble(gas));
                if (amount < 0 || !interactContainer(player, gas, amount, () -> true, true)) {
                    continue;
                }

                return Optional.of(new AffordableFuel(content, amount));
            }
        }
        return Optional.empty();
    }

    public static Optional<AffordableFuel> findAffordableFuel(Player player, Gas selectedGas, ToDoubleFunction<Gas> rawCostFunction) {
        if (selectedGas.isEmpty()) {
            return Optional.empty();
        }

        GasStack selectedContent = GasStack.EMPTY;
        for (IGasCanisterContainer container : CanisterContainerSuppliers.getAllSuppliers(player)) {
            for (int tank = 0; tank < container.getTanks(); tank++) {
                GasStack content = container.getGasInTank(tank);
                if (content.isEmpty() || !content.is(selectedGas)) {
                    continue;
                }

                selectedContent = content;
                break;
            }

            if (!selectedContent.isEmpty()) {
                break;
            }
        }

        if (selectedContent.isEmpty()) {
            return Optional.empty();
        }

        long amount = GasConsumptions.roundUp(rawCostFunction.applyAsDouble(selectedGas));
        if (amount < 0 || !interactContainer(player, selectedGas, amount, () -> true, true)) {
            return Optional.empty();
        }
        return Optional.of(new AffordableFuel(selectedContent, amount));
    }

    public static void applyClientContainerSync(Player player, GasStack gasContent) {
        if (!player.level().isClientSide || player.isCreative() || gasContent.isEmpty() || gasContent.getAmount() <= 0) {
            return;
        }

        List<IGasCanisterContainer> containers = CanisterContainerSuppliers.getAllSuppliers(player);
        if (containers.isEmpty()) {
            return;
        }

        drainContainer(containers, gasContent.getGasType(), gasContent.getAmount(), false, player);
    }

    private static boolean drainContainer(List<IGasCanisterContainer> containers, Gas gasType, long amount, boolean simulate, Player player) {
        if (gasType.isEmpty() || amount <= 0) {
            return true;
        }

        if (simulate) {
            return canCoverCost(containers, gasType, amount, player);
        }

        long remaining = amount;
        Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan = new LinkedHashMap<>();
        for (IGasCanisterContainer container : containers) {
            if (container instanceof GasCanisterContainerContents canister) {
                remaining = planCanisterDrain(canister, gasType, remaining, drainPlan);
            }
            else if (container instanceof GasCanisterPackContainerContents pack) {
                remaining = planPackDrain(pack, gasType, remaining, player, drainPlan);
            }
            else {
                remaining = planGenericDrain(container, gasType, remaining, drainPlan);
            }

            if (remaining <= 0) {
                break;
            }
        }

        return remaining <= 0 && executeDrainPlan(drainPlan, player);
    }

    private static long planCanisterDrain(GasCanisterContainerContents contents, Gas gasType, long remaining, Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
        if (contents.isEmpty() || !contents.getGasInTank(0).is(gasType)) {
            return remaining;
        }

        ItemStack canister = contents.getContainer();
        long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
        GasStack drained = contents.drain(0, requestedDrain, GasAction.SIMULATE);
        if (drained.isEmpty()) {
            return remaining;
        }

        long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained.getAmount(), canister);
        covered = Math.min(covered, remaining);
        if (covered <= 0) {
            return remaining;
        }

        drainPlan.put(contents, List.of(new PlannedContainerDrain(0, drained.copy())));
        return remaining - covered;
    }

    private static long planPackDrain(GasCanisterPackContainerContents contents, Gas gasType, long remaining, Player player, Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
        if (contents.isEmpty()) {
            return remaining;
        }

        List<PlannedContainerDrain> drains = new ArrayList<>();
        for (int slot = 0; slot < GasCanisterPackContainerContents.MAX_COUNT; slot++) {
            if (remaining <= 0) {
                break;
            }
            if (contents.isEmpty(slot) || !contents.getGasInTank(slot).is(gasType)) {
                continue;
            }

            if (contents.getCreatives(slot)) {
                remaining = 0;
                break;
            }

            ItemStack canister = ItemStack.parseOptional(player.level().registryAccess(), contents.getCompoundTag(slot).getCompound(COMPOUND_KEY_CANISTER));
            long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
            GasStack drained = contents.drain(slot, requestedDrain, GasAction.SIMULATE);
            if (drained.isEmpty()) {
                continue;
            }

            long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained.getAmount(), canister);
            covered = Math.min(covered, remaining);
            if (covered <= 0) {
                continue;
            }

            remaining -= covered;
            drains.add(new PlannedContainerDrain(slot, drained.copy()));
        }

        if (drains.isEmpty()) {
            return remaining;
        }

        drainPlan.put(contents, List.copyOf(drains));
        return remaining;
    }

    private static long planGenericDrain(IGasCanisterContainer contents, Gas gasType, long remaining, Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan) {
        List<PlannedContainerDrain> drains = new ArrayList<>();
        for (int tank = 0; tank < contents.getTanks() && remaining > 0; tank++) {
            GasStack storedGas = contents.getGasInTank(tank);
            if (storedGas.isEmpty() || !storedGas.is(gasType)) {
                continue;
            }

            GasStack drained = contents.drain(tank, remaining, GasAction.SIMULATE);
            if (drained.isEmpty() || !drained.is(gasType)) {
                continue;
            }

            long covered = Math.min(drained.getAmount(), remaining);
            if (covered <= 0) {
                continue;
            }

            remaining -= covered;
            drains.add(new PlannedContainerDrain(tank, drained.copyWithAmount(covered)));
        }

        if (!drains.isEmpty()) {
            drainPlan.put(contents, List.copyOf(drains));
        }
        return remaining;
    }

    private static boolean executeDrainPlan(Map<IGasCanisterContainer, List<PlannedContainerDrain>> drainPlan, Player player) {
        ResourceTransaction transaction = new ResourceTransaction();
        drainPlan.forEach((container, drains) -> drains.forEach(drain -> transaction.add(ResourceTransaction.participant(() -> GasStack.matches(container.drain(drain.tank(), drain.gas(), GasAction.SIMULATE), drain.gas()), () -> container.getGasInTank(drain.tank()).copy(), () -> executePlannedDrain(container, drain), snapshot -> restoreContainerTank(container, drain.tank(), snapshot)))));
        if (!transaction.commit()) {
            return false;
        }

        drainPlan.forEach((container, drains) -> {
            if (!(container instanceof GasCanisterPackContainerContents contents)) {
                return;
            }

            drains.forEach(drain -> syncPackMenu(player, contents, drain.tank(), drain.gas().getAmount()));
        });
        return true;
    }

    private static boolean executePlannedDrain(IGasCanisterContainer container, PlannedContainerDrain drain) {
        if (!GasStack.matches(container.drain(drain.tank(), drain.gas(), GasAction.EXECUTE), drain.gas())) {
            return false;
        }
        if (isGenericContainer(container)) {
            container.save();
        }
        return true;
    }

    private static void restoreContainerTank(IGasCanisterContainer container, int tank, GasStack snapshot) {
        GasStack current = container.getGasInTank(tank).copy();
        if (GasStack.matches(current, snapshot)) {
            return;
        }

        if (!current.isEmpty()) {
            GasStack removed = container.drain(tank, current, GasAction.EXECUTE);
            if (!GasStack.matches(removed, current)) {
                throw new IllegalStateException("Failed to clear gas canister container during transaction rollback");
            }
        }
        if (!snapshot.isEmpty() && container.fill(tank, snapshot.copy(), GasAction.EXECUTE) != snapshot.getAmount()) {
            throw new IllegalStateException("Failed to restore gas canister container during transaction rollback");
        }
        if (!GasStack.matches(container.getGasInTank(tank), snapshot)) {
            throw new IllegalStateException("Gas canister container rollback produced an unexpected state");
        }
        if (isGenericContainer(container)) {
            container.save();
        }
    }

    private static boolean isGenericContainer(IGasCanisterContainer container) {
        return !(container instanceof GasCanisterContainerContents) && !(container instanceof GasCanisterPackContainerContents);
    }

    private static void syncPackMenu(Player player, GasCanisterPackContainerContents contents, int slot, long amount) {
        if (!(player instanceof ServerPlayer serverPlayer) || !GasCanisterPackUtils.isCanisterPackMenuOpened(serverPlayer, contents.getContainer()) || !(serverPlayer.containerMenu instanceof GasCanisterPackMenu menu)) {
            return;
        }

        menu.updateCanister(slot, amount);
        CatnipServices.NETWORK.sendToClient(serverPlayer, new GasCanisterPackMenuSyncPacket(slot, amount));
    }

    private static boolean canCoverCost(List<IGasCanisterContainer> containers, Gas gasType, long amount, Player player) {
        long remaining = amount;
        for (IGasCanisterContainer container : containers) {
            if (container instanceof GasCanisterContainerContents contents) {
                if (container.isEmpty() || !contents.getGasInTank(0).is(gasType)) {
                    continue;
                }

                ItemStack canister = contents.getContainer();
                long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
                long drained = contents.drain(0, requestedDrain, GasAction.SIMULATE).getAmount();
                if (drained <= 0) {
                    continue;
                }

                long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained, canister);
                remaining -= Math.min(covered, remaining);
            }
            else if (container instanceof GasCanisterPackContainerContents contents) {
                if (contents.isEmpty()) {
                    continue;
                }

                for (int slot = 0; slot < GasCanisterPackContainerContents.MAX_COUNT && remaining > 0; slot++) {
                    if (contents.isEmpty(slot) || !contents.getGasInTank(slot).is(gasType)) {
                        continue;
                    }

                    if (contents.getCreatives(slot)) {
                        return true;
                    }

                    ItemStack canister = ItemStack.parseOptional(player.level().registryAccess(), contents.getCompoundTag(slot).getCompound(COMPOUND_KEY_CANISTER));
                    long requestedDrain = GasCanisterContainerContents.getEconomizedDrainAmount(remaining, canister);
                    long drained = contents.drain(slot, requestedDrain, GasAction.SIMULATE).getAmount();
                    if (drained <= 0) {
                        continue;
                    }

                    long covered = GasCanisterContainerContents.getLogicalAmountFromEconomizedDrain(drained, canister);
                    remaining -= Math.min(covered, remaining);
                }
            }
            else {
                for (int tank = 0; tank < container.getTanks() && remaining > 0; tank++) {
                    GasStack storedGas = container.getGasInTank(tank);
                    if (storedGas.isEmpty() || !storedGas.is(gasType)) {
                        continue;
                    }

                    GasStack drained = container.drain(tank, remaining, GasAction.SIMULATE);
                    if (drained.isEmpty() || !drained.is(gasType)) {
                        continue;
                    }

                    remaining -= Math.min(drained.getAmount(), remaining);
                }
            }

            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private record PlannedContainerDrain(int tank, GasStack gas) {
        private PlannedContainerDrain {
            gas = gas.copy();
        }
    }

    public record AffordableFuel(GasStack gasContent, long amount) {
        public AffordableFuel {
            gasContent = gasContent.copy();
            if (gasContent.isEmpty()) {
                throw new IllegalArgumentException("Affordable fuel must contain a non-empty gas stack");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("Affordable fuel amount must be non-negative: " + amount);
            }
        }

        public Gas gasType() {
            return gasContent.getGasType();
        }
    }
}
