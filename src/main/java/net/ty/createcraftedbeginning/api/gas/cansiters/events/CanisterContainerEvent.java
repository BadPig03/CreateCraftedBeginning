package net.ty.createcraftedbeginning.api.gas.cansiters.events;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class CanisterContainerEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final Gas gasType;
    private final Supplier<Boolean> executeSupplier;

    private long amount;

    public CanisterContainerEvent(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier) {
        this.player = player;
        this.gasType = gasType;
        this.amount = amount;
        this.executeSupplier = executeSupplier;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull Gas getGasType() {
        return gasType;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public Supplier<Boolean> getExecuteSupplier() {
        return executeSupplier;
    }
}
