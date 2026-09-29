package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonFactory {
    private BalloonFactory() {
    }

    public static ItemStack create(GasStack gas) {
        return create(gas, "");
    }

    public static ItemStack create(GasStack gas, String address) {
        if (gas.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack balloon = BalloonStyles.createRandomBalloon();
        BalloonItem.setGas(balloon, gas);
        if (!address.isBlank()) {
            PackageItem.addAddress(balloon, address);
        }
        return balloon;
    }

    public static ItemStack createOrdered(GasStack gas, String address, int orderId, int linkIndex, boolean finalLink, int packageIndex, boolean finalPackage, @Nullable PackageOrderWithCrafts orderContext) {
        ItemStack balloon = create(gas, address);
        if (balloon.isEmpty()) {
            return ItemStack.EMPTY;
        }

        PackageItem.setOrder(balloon, orderId, linkIndex, finalLink, packageIndex, finalPackage, orderContext);
        return balloon;
    }
}
