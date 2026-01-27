package net.ty.createcraftedbeginning.api.gas.cansiters.events;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

public class GasTypeChangedEvent extends Event {
    private final Player player;
    private final Gas currentGasType;
    private final Gas previousGasType;

    public GasTypeChangedEvent(Player player, Gas currentGasType, Gas previousGasType) {
        this.player = player;
        this.currentGasType = currentGasType;
        this.previousGasType = previousGasType;
    }

    public Player getPlayer() {
        return player;
    }

    public Gas getCurrentGasType() {
        return currentGasType;
    }

    public Gas getPreviousGasType() {
        return previousGasType;
    }
}
