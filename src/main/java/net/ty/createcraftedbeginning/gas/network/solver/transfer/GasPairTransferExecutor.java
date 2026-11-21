package net.ty.createcraftedbeginning.gas.network.solver.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.LongConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPairTransferExecutor {
    private GasPairTransferExecutor() {
    }

    static long executePair(Level level, GasStack gas, GasTransferPlan plan, LongConsumer onFilled) {
        PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(gas, plan.executorDrains(), plan.executorFills(), partial -> settlePair(level, gas, plan, partial, onFilled));
        settlePair(level, gas, plan, execution, onFilled);
        return execution.filledAmounts()[0];
    }

    private static void settlePair(Level level, GasStack gas, GasTransferPlan plan, PooledTransferExecutionResult execution, LongConsumer onFilled) {
        @Nullable Throwable failure = null;
        try {
            long filled = execution.filledAmounts()[0];
            if (filled <= 0) {
                return;
            }

            onFilled.accept(filled);
        }
        catch (RuntimeException | Error caught) {
            failure = caught;
            throw caught;
        }
        finally {
            try {
                plan.restoreRemainder(level, gas, execution);
            }
            catch (RuntimeException | Error recoveryFailure) {
                if (failure == null) {
                    throw recoveryFailure;
                }

                if (recoveryFailure != failure) {
                    failure.addSuppressed(recoveryFailure);
                }
            }
        }
    }
}
