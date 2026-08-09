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
    private final GasRepackagerController controller;

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

    String resolveGasOutputAddress(String originalAddress) {
        updateSignAddress();
        return signBasedAddress.isBlank() ? originalAddress : signBasedAddress;
    }

    void acceptPassThroughPackage(ItemStack packageStack) {
        ItemStack box = packageStack.copy();
        if (PackageItem.hasOrderData(box)) {
            queuedExitingPackages.add(new BigItemStack(box, 1));
            notifyUpdate();
            return;
        }

        heldBox = box;
        animationInward = false;
        animationTicks = CYCLE;
        notifyUpdate();
    }

    void restoreRollbackRemainders(List<ItemStack> remainders) {
        boolean changed = false;
        for (ItemStack remainder : remainders) {
            if (remainder.isEmpty()) {
                continue;
            }

            changed = true;
            if (PackageItem.isPackage(remainder)) {
                queuedExitingPackages.addFirst(new BigItemStack(remainder.copyWithCount(1), remainder.getCount()));
                continue;
            }

            if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.copy());
            }
        }
        if (!changed) {
            return;
        }

        notifyUpdate();
    }

    void enqueuePassThroughBoxes(List<BigItemStack> boxes) {
        if (boxes.isEmpty()) {
            return;
        }

        queuedExitingPackages.addAll(boxes);
        notifyUpdate();
    }

    void enqueueRepackagedBoxes(List<BigItemStack> boxes) {
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
