package net.ty.createcraftedbeginning.api.gas.cansiters;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * This event is fired when a player attempts to consume gas from their canisters.
 * <p>
 * The event can be cancelled to prevent the normal gas consumption behavior. However,
 * even when cancelled, gas will still be consumed with an amount of 0. This allows
 * for tracking consumption attempts while preventing actual gas deduction.
 * </p>
 * <p>
 * The amount to be consumed can be modified using {@link #setAmount(long)}.
 * </p>
 */
@SuppressWarnings("unused")
public class GasConsumptionEvent extends Event implements ICancellableEvent {
    private final Player player;
    private final Gas targetGas;
    private final Supplier<Boolean> executeSupplier;

    private long amount;

    /**
     * Constructs a new GasConsumptionEvent.
     *
     * @param player          the player attempting to consume gas (cannot be null)
     * @param targetGas       the type of gas to be consumed (cannot be null)
     * @param amount          the amount of gas to be consumed
     * @param executeSupplier the condition supplier that determines if consumption should execute
     */
    public GasConsumptionEvent(@NotNull Player player, @NotNull Gas targetGas, long amount, Supplier<Boolean> executeSupplier) {
        this.player = player;
        this.targetGas = targetGas;
        this.amount = amount;
        this.executeSupplier = executeSupplier;
    }

    /**
     * Gets the player who is attempting to consume gas.
     *
     * @return the player consuming gas (never null)
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Gets the type of gas being consumed.
     *
     * @return the target gas type (never null)
     */
    public @NotNull Gas getTargetGas() {
        return targetGas;
    }

    /**
     * Gets the amount of gas to be consumed.
     * <p>
     * This amount may be modified by event handlers using {@link #setAmount(long)}.
     * </p>
     *
     * @return the current amount of gas to be consumed
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Gets the execute supplier that determines if gas consumption should proceed.
     * <p>
     * This supplier is typically used for conditional consumption scenarios,
     * such as periodic operations that only consume gas on specific ticks.
     * </p>
     *
     * @return the execute condition supplier, or null if no condition is set
     */
    public Supplier<Boolean> getExecuteSupplier() {
        return executeSupplier;
    }

    /**
     * Sets a new amount of gas to be consumed.
     * <p>
     * Event handlers can use this method to modify the consumption amount.
     * Setting the amount to 0 effectively prevents gas from being consumed
     * while still tracking the consumption attempt.
     * </p>
     *
     * @param newAmount the new amount of gas to consume
     */
    public void setAmount(long newAmount) {
        amount = newAmount;
    }
}
