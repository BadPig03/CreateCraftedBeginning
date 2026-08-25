package net.ty.createcraftedbeginning.compat.computercraft;

import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.computercraft.events.PackageEvent;
import com.simibubi.create.compat.computercraft.events.RepackageEvent;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ComputerCraftPackagerCompat {
    private ComputerCraftPackagerCompat() {
    }

    public static void addBehaviour(PackagerBlockEntity packager, List<BlockEntityBehaviour> behaviours) {
        packager.computerBehaviour = ComputerCraftProxy.behaviour(packager);
        behaviours.add(packager.computerBehaviour);
    }

    public static void emitPackageReceived(PackagerBlockEntity packager, ItemStack box) {
        if (packager.computerBehaviour == null) {
            return;
        }

        packager.computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_received"));
    }

    public static void emitPackageCreated(PackagerBlockEntity packager, ItemStack box) {
        if (packager.computerBehaviour == null) {
            return;
        }

        packager.computerBehaviour.prepareComputerEvent(new PackageEvent(box, "package_created"));
    }

    public static void emitRepackage(PackagerBlockEntity packager, List<BigItemStack> boxes) {
        if (packager.computerBehaviour == null || !packager.computerBehaviour.hasAttachedComputer()) {
            return;
        }

        boxes.forEach(box -> packager.computerBehaviour.prepareComputerEvent(new RepackageEvent(box.stack, box.count)));
    }
}
