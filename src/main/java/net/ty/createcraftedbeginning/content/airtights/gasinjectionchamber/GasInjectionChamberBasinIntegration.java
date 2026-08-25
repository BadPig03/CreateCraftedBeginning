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
import net.ty.createcraftedbeginning.platform.BasinTransactionBridge;
import net.ty.createcraftedbeginning.platform.BasinTransactionBridge.TransactionHandle;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberBasinIntegration {
    private static volatile boolean failureLogged;

    private GasInjectionChamberBasinIntegration() {
    }

    public static void onBasinContentsChanged(BasinBlockEntity basin) {
        Level level = basin.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (!(level.getBlockEntity(basin.getBlockPos().above(2)) instanceof GasInjectionChamberBlockEntity chamber)) {
            return;
        }

        chamber.scheduleBasinCheck();
    }

    static @Nullable TransactionView getTransactionView(BasinBlockEntity basin) {
        TransactionHandle transaction = BasinTransactionBridge.createHandle(basin);
        if (transaction == null) {
            logTransactionAccessFailure();
            return null;
        }

        SmartFluidTankBehaviour inputTank = basin.getTanks().getFirst();
        SmartFluidTankBehaviour outputTank = basin.getTanks().getSecond();
        return new TransactionView(inputTank, outputTank, transaction);
    }

    private static void logTransactionAccessFailure() {
        if (failureLogged) {
            return;
        }

        failureLogged = true;
        CCBAPI.LOGGER.error("Gas injection chamber integration with Create's BasinBlockEntity transaction state is unavailable.");
    }

    record TransactionView(SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank, TransactionHandle transaction) {
        TransactionSnapshot snapshot(Provider provider) {
            List<FluidStack> outputBuffer = transaction.snapshotFluidOverflow();
            FluidTankSnapshot tanks = MachineResourceSnapshots.snapshotFluidTanks(provider, inputTank, outputTank);
            return new TransactionSnapshot(tanks, outputBuffer);
        }

        void restore(Provider provider, TransactionSnapshot snapshot) {
            MachineResourceSnapshots.restoreFluidTanks(provider, snapshot.tanks(), inputTank, outputTank);
            transaction.restoreFluidOverflow(snapshot.outputBuffer());
        }
    }

    record TransactionSnapshot(FluidTankSnapshot tanks, List<FluidStack> outputBuffer) {}
}
