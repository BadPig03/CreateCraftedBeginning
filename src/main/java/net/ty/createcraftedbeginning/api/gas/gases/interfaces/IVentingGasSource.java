package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Marks a gas source whose already-drained remainder cannot be inserted back into it.
 * Gas networks treat an unaccepted remainder from this kind of source as vented instead
 * of permanently pausing the network while attempting a rollback.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IVentingGasSource {}
