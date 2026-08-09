package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour.RequestType;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasLogisticsUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasFactoryGaugeRestockController {
    private GasFactoryGaugeRestockController() {
    }

    static Result request(UUID network, ItemStack gasToken, GasPackagerBlockEntity packager, int targetAmount, int promisedAmount, int storedAmount, String recipeAddress) {
        IdentifiedInventory excludedInventory = packager.getIdentifiedGasInventory();
        if (excludedInventory == null) {
            return Result.NONE;
        }

        int available = GasLogisticsUtils.getUniqueStockOf(network, gasToken, excludedInventory);
        if (available <= 0) {
            return Result.failed();
        }

        int missing = Math.max(0, targetAmount - promisedAmount - storedAmount);
        int cycleLimit = GasRequestUtils.toLogisticsAmount(Math.max(1, BalloonUtils.getCapacity()) * 9);
        int orderAmount = Math.min(Math.min(missing, available), cycleLimit);
        if (orderAmount <= 0) {
            return Result.NONE;
        }

        BigItemStack orderedGas = new BigItemStack(gasToken, orderAmount);
        PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(List.of(orderedGas));
        boolean accepted = LogisticsManager.broadcastPackageRequest(network, RequestType.RESTOCK, order, excludedInventory, recipeAddress);
        return accepted ? Result.accepted(orderedGas) : Result.failed();
    }

    enum Effect {
        NONE,
        SUCCESS,
        FAILURE
    }

    record Result(Effect effect, @Nullable BigItemStack promisedGas) {
        private static final Result NONE = new Result(Effect.NONE, null);

        private static Result accepted(BigItemStack orderedGas) {
            return new Result(Effect.SUCCESS, orderedGas);
        }

        private static Result failed() {
            return new Result(Effect.FAILURE, null);
        }
    }
}
