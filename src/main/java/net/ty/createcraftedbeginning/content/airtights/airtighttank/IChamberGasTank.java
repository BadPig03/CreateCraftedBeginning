package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.ty.createcraftedbeginning.api.gas.GasTank;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;

public interface IChamberGasTank {
    GasTank getTankInventory();

    IGasHandler getCapability();

    int getWidth();

    IChamberGasTank getControllerBE();

    boolean isController();

    boolean isRemoved();
}
