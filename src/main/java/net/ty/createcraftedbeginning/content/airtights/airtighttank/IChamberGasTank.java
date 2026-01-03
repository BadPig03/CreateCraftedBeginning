package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.ty.createcraftedbeginning.api.gas.gases.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;

public interface IChamberGasTank {
    GasTank getTankInventory();

    IGasHandler getCapability();

    int getWidth();

    IChamberGasTank getControllerBE();

    boolean isController();

    boolean isRemoved();
}
