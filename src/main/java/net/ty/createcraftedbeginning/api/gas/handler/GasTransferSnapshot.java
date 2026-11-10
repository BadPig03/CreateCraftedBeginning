package net.ty.createcraftedbeginning.api.gas.handler;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A checkpoint for one subsequent EXECUTE operation on a gas handler. Restoration
 * must restore the captured inventory, pressure parameters and transactional side
 * effects without applying current insertion or extraction permissions. A handler
 * with irreversible effects must not offer a checkpoint for those operations.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface GasTransferSnapshot {
    void restore();

    /**
     * Refreshes notifications after resource restoration has succeeded. A failure
     * here does not make the restored inventory uncertain and must remain visible
     * as a suppressed failure of the original operation.
     */
    default void afterRestore() {
    }
}
