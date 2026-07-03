package net.ty.createcraftedbeginning.api.gascanisters.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CanisterContainerEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final Gas gasType;
    private final Supplier<Boolean> executeSupplier;
    private final boolean simulate;

    private long amount;

    public CanisterContainerEvent(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier, boolean simulate) {
        this.player = player;
        this.gasType = gasType;
        this.amount = amount;
        this.executeSupplier = executeSupplier;
        this.simulate = simulate;
    }

    public Player getPlayer() {
        return player;
    }

    public Gas getGasType() {
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

    public boolean isSimulate() {
        return simulate;
    }
}
