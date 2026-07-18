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
@SuppressWarnings("unused")
public class CanisterContainerEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final Gas gasType;
    private final Supplier<Boolean> executeSupplier;
    private final boolean simulate;

    private long amount;

    /**
     * Creates a new {@code CanisterContainerEvent} instance.
     *
     * @param player          the player performing the operation
     * @param gasType         the gas type to inspect or process
     * @param amount          the amount to use
     * @param executeSupplier the supplier used to obtain the execute
     * @param simulate        whether the operation should be simulated
     */
    public CanisterContainerEvent(Player player, Gas gasType, long amount, Supplier<Boolean> executeSupplier, boolean simulate) {
        this.player = player;
        this.gasType = gasType;
        this.amount = amount;
        this.executeSupplier = executeSupplier;
        this.simulate = simulate;
    }

    /**
     * Returns the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the gas type.
     *
     * @return the gas type
     */
    public Gas getGasType() {
        return gasType;
    }

    /**
     * Returns the amount.
     *
     * @return the amount
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the amount.
     *
     * @param amount the amount to use
     */
    public void setAmount(long amount) {
        this.amount = amount;
    }

    /**
     * Returns the execute supplier.
     *
     * @return the execute supplier
     */
    public Supplier<Boolean> getExecuteSupplier() {
        return executeSupplier;
    }

    /**
     * Checks whether the operation is configured for simulation.
     *
     * @return {@code true} if the operation is configured for simulation; otherwise {@code false}
     */
    public boolean isSimulate() {
        return simulate;
    }
}
