package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager;

import com.simibubi.create.compat.computercraft.events.RepackageEvent;
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
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasRepackagerBlockEntity extends RepackagerBlockEntity {
    protected final GasRepackagerController controller;

    public GasRepackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controller = new GasRepackagerController(this);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_REPACKAGER.get(), (be, context) -> be.inventory);
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        return PackageItem.isPackage(box) && super.unwrapBox(box, simulate);
    }

    @Override
    protected void attemptToRepackage(IItemHandler targetInv) {
        controller.attemptToRepackage(targetInv);
    }

    public String resolveGasOutputAddress(String originalAddress) {
        updateSignAddress();
        return signBasedAddress.isBlank() ? originalAddress : signBasedAddress;
    }

    public void acceptPassThroughPackage(ItemStack packageStack) {
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

    public void restoreRollbackRemainders(List<ItemStack> remainders) {
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

            if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.copy());
            }
        }
        if (!restoredAnyRemainder) {
            return;
        }

        notifyUpdate();
    }

    public void enqueuePassThroughBoxes(List<BigItemStack> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        queuedExitingPackages.addAll(boxes);
        notifyUpdate();
    }

    public void enqueueRepackagedBoxes(List<BigItemStack> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        if (computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            boxes.forEach(box -> computerBehaviour.prepareComputerEvent(new RepackageEvent(box.stack, box.count)));
        }
        queuedExitingPackages.addAll(boxes);
        notifyUpdate();
    }
}
