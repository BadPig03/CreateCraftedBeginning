package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.compat.computercraft.ComputerCraftPackagerCompat;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPressureSemantics;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasRepackagerBlockEntity extends RepackagerBlockEntity {
    private final GasRepackagerController controller;

    public GasRepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controller = new GasRepackagerController(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_REPACKAGER.get(), (repackager, context) -> repackager.inventory);
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        return PackageItem.isPackage(box) && super.unwrapBox(box, simulate);
    }

    @Override
    protected void attemptToRepackage(IItemHandler targetInv) {
        controller.attemptToRepackage(targetInv);
    }

    void attemptVanillaItemRepackage(IItemHandler targetInv) {
        super.attemptToRepackage(new NonGasPackageItemHandler(targetInv));
    }

    long ambientPressurePa() {
        if (level == null) {
            return GasPressure.VACUUM_PA;
        }

        return BalloonPressureSemantics.ambientPressurePa(level, worldPosition);
    }

    String resolveGasOutputAddress(String originalAddress) {
        updateSignAddress();
        if (signBasedAddress.isBlank()) {
            return originalAddress;
        }

        return signBasedAddress;
    }

    void acceptPassThroughPackage(ItemStack packageStack) {
        ItemStack packageCopy = packageStack.copy();
        if (PackageItem.hasOrderData(packageCopy)) {
            queuedExitingPackages.add(new BigItemStack(packageCopy, 1));
            notifyUpdate();
            return;
        }

        heldBox = packageCopy;
        animationInward = false;
        animationTicks = CYCLE;
        notifyUpdate();
    }

    void restoreRollbackRemainders(List<ItemStack> remainders) {
        boolean restoredAnyRemainder = false;
        for (ItemStack remainder : remainders) {
            if (remainder.isEmpty()) {
                continue;
            }

            restoredAnyRemainder = true;
            if (PackageItem.isPackage(remainder)) {
                queuedExitingPackages.addFirst(new BigItemStack(remainder.copyWithCount(1), remainder.getCount()));
                continue;
            }

            if (level == null) {
                continue;
            }

            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.copy());
        }
        if (!restoredAnyRemainder) {
            return;
        }

        notifyUpdate();
    }

    void enqueueRepackagedBoxes(List<BigItemStack> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        ComputerCraftPackagerCompat.emitRepackage(this, boxes);
        queuedExitingPackages.addAll(boxes);
        notifyUpdate();
    }

    private record NonGasPackageItemHandler(IItemHandler delegate) implements IItemHandler {
        @Override
        public int getSlots() {
            return delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack stack = delegate.getStackInSlot(slot);
            if (BalloonItem.containsGas(stack)) {
                return ItemStack.EMPTY;
            }

            return stack;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (BalloonItem.containsGas(stack)) {
                return stack;
            }

            return delegate.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack simulated = delegate.extractItem(slot, amount, true);
            if (BalloonItem.containsGas(simulated)) {
                return ItemStack.EMPTY;
            }

            return delegate.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !BalloonItem.containsGas(stack) && delegate.isItemValid(slot, stack);
        }
    }
}
