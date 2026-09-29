package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.platform.BasinTransactionBridge;
import net.ty.createcraftedbeginning.platform.BasinTransactionBridge.TransactionHandle;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots.FluidTankSnapshot;
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
        return new TransactionView(basin.getInputInventory(), basin.getOutputInventory(), inputTank, outputTank, transaction);
    }

    private static void logTransactionAccessFailure() {
        if (failureLogged) {
            return;
        }

        failureLogged = true;
        CCBAPI.LOGGER.error("Gas injection chamber integration with Create's BasinBlockEntity transaction state is unavailable.");
    }

    record TransactionView(SmartInventory inputInventory, SmartInventory outputInventory, SmartFluidTankBehaviour inputTank, SmartFluidTankBehaviour outputTank, TransactionHandle transaction) {
        TransactionSnapshot snapshot(Provider provider) {
            List<ItemStack> inputItems = MachineResourceSnapshots.copyItems(inputInventory);
            List<ItemStack> outputItems = MachineResourceSnapshots.copyItems(outputInventory);
            List<ItemStack> itemOutputBuffer = transaction.snapshotItemOverflow();
            List<FluidStack> fluidOutputBuffer = transaction.snapshotFluidOverflow();
            FluidTankSnapshot tanks = MachineResourceSnapshots.snapshotFluidTanks(provider, inputTank, outputTank);
            return new TransactionSnapshot(inputItems, outputItems, tanks, itemOutputBuffer, fluidOutputBuffer);
        }

        void restore(Provider provider, TransactionSnapshot snapshot) {
            MachineResourceSnapshots.restoreItems(inputInventory, snapshot.inputItems());
            MachineResourceSnapshots.restoreItems(outputInventory, snapshot.outputItems());
            MachineResourceSnapshots.restoreFluidTanks(provider, snapshot.tanks(), inputTank, outputTank);
            transaction.restoreItemOverflow(snapshot.itemOutputBuffer());
            transaction.restoreFluidOverflow(snapshot.fluidOutputBuffer());
        }
    }

    record TransactionSnapshot(List<ItemStack> inputItems, List<ItemStack> outputItems, FluidTankSnapshot tanks, List<ItemStack> itemOutputBuffer, List<FluidStack> fluidOutputBuffer) {}
}
