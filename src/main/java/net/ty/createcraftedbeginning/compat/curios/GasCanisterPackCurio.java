package net.ty.createcraftedbeginning.compat.curios;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasCanisterPackCurio implements ICurio {
    private final ItemStack stack;

    GasCanisterPackCurio(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }

    @Override
    public void onEquip(SlotContext context, ItemStack previousStack) {
        if (!(context.entity() instanceof Player player)) {
            return;
        }

        CanisterContainerSuppliers.invalidateCache(player);
    }

    @Override
    public void onUnequip(SlotContext context, ItemStack newStack) {
        if (!(context.entity() instanceof Player player)) {
            return;
        }

        CanisterContainerSuppliers.invalidateCache(player);
    }

    @Override
    public boolean canEquip(SlotContext context) {
        return CuriosCompat.GAS_CANISTER_PACK_SLOT.equals(context.identifier()) && !context.cosmetic();
    }
}
