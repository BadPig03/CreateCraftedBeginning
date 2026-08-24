package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.core.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.MachineResourceSnapshots.FluidTankSnapshot;
import net.ty.createcraftedbeginning.platform.access.BasinTransactionAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberBasinCompat {
    private static volatile boolean accessVerified;
    private static volatile boolean failureLogged;
    private static volatile boolean hookVerified;

    private GasInjectionChamberBasinCompat() {
    }

    public static void markAccessVerified() {
        accessVerified = true;
    }

    public static void onBasinContentsChanged(BasinBlockEntity basin) {
        markHookVerified();
        Level level = basin.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (!(level.getBlockEntity(basin.getBlockPos().above(2)) instanceof GasInjectionChamberBlockEntity chamber)) {
            return;
        }

        chamber.scheduleBasinCheck();
    }

    static boolean isHookVerified() {
        return hookVerified;
    }

    static @Nullable TransactionView getTransactionView(BasinBlockEntity basin) {
        if (!(basin instanceof BasinTransactionAccess transactionAccess)) {
            logTransactionAccessFailure();
            return null;
        }

        try {
            SmartFluidTankBehaviour inputTank = basin.getTanks().getFirst();
            SmartFluidTankBehaviour outputTank = basin.getTanks().getSecond();
            if (!accessVerified) {
                transactionAccess.ccb$copyTransactionFluidOverflow();
                markAccessVerified();
            }
            return new TransactionView(inputTank, outputTank, transactionAccess);
        } catch (RuntimeException | LinkageError exception) {
            logTransactionAccessFailure(exception);
            return null;
        }
    }

    private static void markHookVerified() {
        hookVerified = true;
    }

    private static void logTransactionAccessFailure() {
        if (failureLogged) {
            return;
        }

        failureLogged = true;
        CCBAPI.LOGGER.error("Gas injection chamber integration with Create's BasinBlockEntity transaction state is unavailable.");
    }

    private static void logTransactionAccessFailure(Throwable throwable) {
        if (failureLogged) {
            return;
        }

        failureLogged = true;
        CCBAPI.LOGGER.error("Gas injection chamber integration with Create's BasinBlockEntity transaction state is unavailable.", throwable);
    }

    record TransactionView(SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank, BasinTransactionAccess transactionAccess) {
        TransactionSnapshot snapshot(Provider provider) {
            List<FluidStack> outputBuffer = transactionAccess.ccb$copyTransactionFluidOverflow();
            FluidTankSnapshot tanks = MachineResourceSnapshots.snapshotFluidTanks(provider, inputTank, outputTank);
            return new TransactionSnapshot(tanks, outputBuffer);
        }

        void restore(Provider provider, TransactionSnapshot snapshot) {
            MachineResourceSnapshots.restoreFluidTanks(provider, snapshot.tanks(), inputTank, outputTank);
            transactionAccess.ccb$restoreTransactionFluidOverflow(snapshot.outputBuffer());
        }
    }

    record TransactionSnapshot(FluidTankSnapshot tanks, List<FluidStack> outputBuffer) {}
}
