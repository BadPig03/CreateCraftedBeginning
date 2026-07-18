package net.ty.createcraftedbeginning.api.gascanisters.events;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasTypeChangedEvent extends Event {
    private final Player player;
    private final Gas currentGasType;
    private final Gas previousGasType;

    /**
     * Creates a new {@code GasTypeChangedEvent} instance.
     *
     * @param player          the player performing the operation
     * @param currentGasType  the current gas type to use
     * @param previousGasType the previous gas type to use
     */
    public GasTypeChangedEvent(Player player, Gas currentGasType, Gas previousGasType) {
        this.player = player;
        this.currentGasType = currentGasType;
        this.previousGasType = previousGasType;
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
     * Returns the current gas type.
     *
     * @return the current gas type
     */
    public Gas getCurrentGasType() {
        return currentGasType;
    }

    /**
     * Returns the previous gas type.
     *
     * @return the previous gas type
     */
    public Gas getPreviousGasType() {
        return previousGasType;
    }
}
