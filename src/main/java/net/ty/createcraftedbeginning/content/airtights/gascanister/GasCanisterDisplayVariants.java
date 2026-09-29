package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterContainerContents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCanisterDisplayVariants {
    private GasCanisterDisplayVariants() {
    }

    public static List<ItemStack> createCanisterVariants() {
        List<Gas> gases = GasRegistries.GAS_REGISTRY.stream().filter(gas -> !gas.isEmpty()).toList();
        List<ItemStack> canisters = new ArrayList<>(List.of(new ItemStack(CCBItems.GAS_CANISTER.asItem())));
        gases.forEach(gasEntry -> {
            ItemStack canister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
            if (canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents contents) {
                contents.fill(0, new GasStack(gasEntry, contents.getTankMaxAmount(0)), GasAction.EXECUTE);
            }
            canisters.add(canister);
        });

        canisters.add(new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem()));
        gases.forEach(gasEntry -> {
            ItemStack canister = new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem());
            if (canister.getCapability(CanisterCapabilities.ITEM) instanceof CreativeGasCanisterContainerContents contents) {
                contents.setGasInTank(0, new GasStack(gasEntry, contents.getTankMaxAmount(0)));
            }
            canisters.add(canister);
        });
        return canisters;
    }
}
