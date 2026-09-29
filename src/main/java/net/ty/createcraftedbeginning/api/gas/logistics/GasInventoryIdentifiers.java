package net.ty.createcraftedbeginning.api.gas.logistics;

import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.packager.InventoryIdentifier.MultiFace;
import com.simibubi.create.api.registry.SimpleRegistry;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInventoryIdentifiers {
    public static final SimpleRegistry<Block, Finder> REGISTRY = SimpleRegistry.create();

    private GasInventoryIdentifiers() {
    }

    @Nullable
    public static InventoryIdentifier get(Level level, BlockFace face) {
        GasHandler targetHandler = level.getCapability(GasCapabilities.BLOCK, face.getPos(), face.getFace());
        if (targetHandler == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(face.getPos());
        if (blockEntity instanceof GasInventoryIdentifierProvider provider) {
            return provider.getGasInventoryIdentifier(face.getFace());
        }

        BlockState state = level.getBlockState(face.getPos());
        Finder finder = REGISTRY.get(state);
        if (finder != null) {
            return finder.find(level, state, face);
        }

        return fallback(level, face, targetHandler);
    }

    public static void register(Block block, Finder finder) {
        REGISTRY.register(block, finder);
    }

    @Nullable
    private static InventoryIdentifier fallback(Level level, BlockFace face, GasHandler targetHandler) {
        if (targetHandler.getTanks() <= 0) {
            return null;
        }

        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        for (Direction direction : Iterate.directions) {
            GasHandler candidate = level.getCapability(GasCapabilities.BLOCK, face.getPos(), direction);
            if (candidate == null) {
                continue;
            }

            if (!identifiesSameStorage(targetHandler, candidate)) {
                return null;
            }

            sides.add(direction);
        }

        if (!sides.contains(face.getFace())) {
            return null;
        }

        return new MultiFace(face.getPos(), Set.copyOf(sides));
    }

    private static boolean identifiesSameStorage(GasHandler first, GasHandler second) {
        if (first == second) {
            return true;
        }

        if (first instanceof GasPressureCompartment firstCompartment && second instanceof GasPressureCompartment secondCompartment) {
            return firstCompartment.getCompartmentIdentity() == secondCompartment.getCompartmentIdentity();
        }

        if (!(first instanceof GasStorageHandler firstStorage) || !(second instanceof GasStorageHandler secondStorage)) {
            return false;
        }

        int tanks = firstStorage.getTanks();
        if (tanks != secondStorage.getTanks()) {
            return false;
        }

        boolean[] matched = new boolean[tanks];
        for (int firstTank = 0; firstTank < tanks; firstTank++) {
            Object identity = firstStorage.getPressureCompartment(firstTank).getCompartmentIdentity();
            boolean found = false;
            for (int secondTank = 0; secondTank < tanks; secondTank++) {
                if (matched[secondTank] || identity != secondStorage.getPressureCompartment(secondTank).getCompartmentIdentity()) {
                    continue;
                }

                matched[secondTank] = true;
                found = true;
                break;
            }

            if (!found) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    public interface Finder {
        @Nullable InventoryIdentifier find(Level level, BlockState state, BlockFace face);
    }
}
