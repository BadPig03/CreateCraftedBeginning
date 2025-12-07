package net.ty.createcraftedbeginning.api.gas;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTankMultiBlockEntityContainer;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasTankMultiBlockEntityContainer.iGas;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public class GasConnectivityHandler {
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void formMulti(T be) {
        SearchCache<T> cache = new SearchCache<>();
        List<T> frontier = new ArrayList<>();
        frontier.add(be);
        formMulti(be.getType(), be.getLevel(), cache, frontier);
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void formMulti(BlockEntityType<?> type, BlockGetter level, SearchCache<T> cache, @NotNull List<T> frontier) {
        PriorityQueue<Pair<Integer, T>> creationQueue = makeCreationQueue();
        Set<BlockPos> visited = new HashSet<>();
        Axis mainAxis = frontier.getFirst().getMainConnectionAxis();
        int minX = mainAxis == Axis.Y ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        int minY = mainAxis == Axis.Y ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minZ = mainAxis == Axis.Y ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        for (T be : frontier) {
            BlockPos pos = be.getBlockPos();
            minX = Math.min(pos.getX(), minX);
            minY = Math.min(pos.getY(), minY);
            minZ = Math.min(pos.getZ(), minZ);
        }
        if (mainAxis == Axis.Y) {
            minX -= frontier.getFirst().getMaxWidth();
        }
        if (mainAxis != Axis.Y) {
            minY -= frontier.getFirst().getMaxWidth();
        }
        if (mainAxis == Axis.Y) {
            minZ -= frontier.getFirst().getMaxWidth();
        }

        while (!frontier.isEmpty()) {
            T part = frontier.removeFirst();
            BlockPos partPos = part.getBlockPos();
            if (visited.contains(partPos)) {
                continue;
            }

            visited.add(partPos);
            int amount = tryToFormNewMulti(part, cache, true);
            if (amount > 1) {
                creationQueue.add(Pair.of(amount, part));
            }

            for (Axis axis : Iterate.axes) {
                Direction dir = Direction.get(AxisDirection.NEGATIVE, axis);
                BlockPos next = partPos.relative(dir);
                if (next.getX() <= minX || next.getY() <= minY || next.getZ() <= minZ) {
                    continue;
                }
                if (visited.contains(next)) {
                    continue;
                }

                T nextBe = partAt(type, level, next);
                if (nextBe == null) {
                    continue;
                }
                if (nextBe.isRemoved()) {
                    continue;
                }

                frontier.add(nextBe);
            }
        }

        visited.clear();
        while (!creationQueue.isEmpty()) {
            Pair<Integer, T> next = creationQueue.poll();
            T toCreate = next.getValue();
            if (visited.contains(toCreate.getBlockPos())) {
                continue;
            }

            visited.add(toCreate.getBlockPos());
            tryToFormNewMulti(toCreate, cache, false);
        }
    }

    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> boolean isConnected(@NotNull BlockGetter level, BlockPos pos, BlockPos other) {
        T one = checked(level.getBlockEntity(pos));
        T two = checked(level.getBlockEntity(other));
        return one != null && two != null && one.getController().equals(two.getController());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> T checked(BlockEntity be) {
        return be instanceof IGasTankMultiBlockEntityContainer ? (T) be : null;
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> int tryToFormNewMulti(@NotNull T be, SearchCache<T> cache, boolean simulate) {
        int bestWidth = 1;
        int bestAmount = -1;
        if (!be.isController()) {
            return 0;
        }

        int radius = be.getMaxWidth();
        for (int w = 1; w <= radius; w++) {
            int amount = tryToFormNewMultiOfWidth(be, w, cache, true);
            if (amount < bestAmount) {
                continue;
            }

            bestWidth = w;
            bestAmount = amount;
        }

        if (!simulate) {
            int beWidth = be.getWidth();
            if (beWidth == bestWidth && beWidth * beWidth * be.getHeight() == bestAmount) {
                return bestAmount;
            }

            splitMultiAndInvalidate(be, cache);
            if (be instanceof iGas iGasBE && iGasBE.hasTank()) {
                iGasBE.setTankSize(0, bestAmount);
            }

            tryToFormNewMultiOfWidth(be, bestWidth, cache, false);
            be.preventConnectivityUpdate();
            be.setWidth(bestWidth);
            be.setHeight(bestAmount / bestWidth / bestWidth);
            be.notifyMultiUpdated();
        }
        return bestAmount;
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> int tryToFormNewMultiOfWidth(@NotNull T be, int width, SearchCache<T> cache, boolean simulate) {
        int amount = 0;
        int height = 0;
        BlockEntityType<?> type = be.getType();
        Level level = be.getLevel();
        if (level == null) {
            return 0;
        }

        BlockPos origin = be.getBlockPos();
        IGasTank beTank = null;
        GasStack gas = GasStack.EMPTY;
        if (be instanceof iGas iGas && iGas.hasTank()) {
            beTank = iGas.getTank(0);
            gas = beTank.getGasStack();
        }
        Axis axis = be.getMainConnectionAxis();

        Search:
        for (int yOffset = 0; yOffset < be.getMaxLength(axis, width); yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };
                    Optional<T> part = cache.getOrCache(type, level, pos);
                    if (part.isEmpty()) {
                        break Search;
                    }

                    T controller = part.get();
                    int otherWidth = controller.getWidth();
                    if (otherWidth > width) {
                        break Search;
                    }
                    if (otherWidth == width && controller.getHeight() == be.getMaxLength(axis, width)) {
                        break Search;
                    }

                    Axis conAxis = controller.getMainConnectionAxis();
                    if (axis != conAxis) {
                        break Search;
                    }

                    BlockPos conPos = controller.getBlockPos();
                    if (!conPos.equals(origin)) {
                        if (axis == Axis.Y) {
                            if (conPos.getX() < origin.getX()) {
                                break Search;
                            }
                            if (conPos.getZ() < origin.getZ()) {
                                break Search;
                            }
                            if (conPos.getX() + otherWidth > origin.getX() + width) {
                                break Search;
                            }
                            if (conPos.getZ() + otherWidth > origin.getZ() + width) {
                                break Search;
                            }
                        }
                        else {
                            if (axis == Axis.Z && conPos.getX() < origin.getX()) {
                                break Search;
                            }
                            if (conPos.getY() < origin.getY()) {
                                break Search;
                            }
                            if (axis == Axis.X && conPos.getZ() < origin.getZ()) {
                                break Search;
                            }
                            if (axis == Axis.Z && conPos.getX() + otherWidth > origin.getX() + width) {
                                break Search;
                            }
                            if (conPos.getY() + otherWidth > origin.getY() + width) {
                                break Search;
                            }
                            if (axis == Axis.X && conPos.getZ() + otherWidth > origin.getZ() + width) {
                                break Search;
                            }
                        }
                    }
                    if (controller instanceof iGas iGasContainer && iGasContainer.hasTank()) {
                        GasStack otherGas = iGasContainer.getGas(0);
                        if (!gas.isEmpty() && !otherGas.isEmpty() && !GasStack.isSameGasSameComponents(gas, otherGas)) {
                            break Search;
                        }
                    }
                }
            }
            amount += width * width;
            height++;
        }

        if (simulate) {
            return amount;
        }

        Object extraData = be.getExtraData();
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };

                    T part = partAt(type, level, pos);
                    if (part == null) {
                        continue;
                    }
                    if (part == be) {
                        continue;
                    }

                    extraData = be.modifyExtraData(extraData);
                    if (part instanceof iGas iGasPart && iGasPart.hasTank()) {
                        IGasTank tankAt = iGasPart.getTank(0);
                        GasStack gasAt = tankAt.getGasStack();
                        if (!gasAt.isEmpty()) {
                            if (be instanceof iGas iGasBE && iGasBE.hasTank() && beTank != null) {
                                beTank.fill(gasAt, GasAction.EXECUTE);
                            }
                        }
                        tankAt.drain(tankAt.getCapacity(), GasAction.EXECUTE);
                    }

                    splitMultiAndInvalidate(part, cache);
                    part.setController(origin);
                    part.preventConnectivityUpdate();
                    cache.put(pos, be);
                    part.setHeight(height);
                    part.setWidth(width);
                    part.notifyMultiUpdated();
                }
            }
        }

        be.setExtraData(extraData);
        be.notifyMultiUpdated();
        return amount;
    }

    @Contract(value = " -> new", pure = true)
    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> @NotNull PriorityQueue<Pair<Integer, T>> makeCreationQueue() {
        return new PriorityQueue<>((one, two) -> two.getKey() - one.getKey());
    }

    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void splitMulti(T be) {
        splitMultiAndInvalidate(be, null);
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void splitMultiAndInvalidate(@NotNull T be, @Nullable SearchCache<T> cache) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        be = be.getControllerBE();
        if (be == null) {
            return;
        }

        int height = be.getHeight();
        int width = be.getWidth();
        if (width == 1 && height == 1) {
            return;
        }

        BlockPos origin = be.getBlockPos();
        Axis axis = be.getMainConnectionAxis();
        GasStack toDistribute = GasStack.EMPTY;
        long maxCapacity = 0;
        if (be instanceof iGas iGasBE && iGasBE.hasTank()) {
            toDistribute = iGasBE.getGas(0);
            maxCapacity = iGasBE.getTankSize(0);
            if (!toDistribute.isEmpty() && !be.isRemoved()) {
                toDistribute.shrink(maxCapacity);
            }
            iGasBE.setTankSize(0, 1);
        }

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };

                    T partAt = partAt(be.getType(), level, pos);
                    if (partAt == null) {
                        continue;
                    }
                    if (!partAt.getController().equals(origin)) {
                        continue;
                    }

                    T controllerBE = partAt.getControllerBE();
                    partAt.setExtraData(controllerBE == null ? null : controllerBE.getExtraData());
                    partAt.removeController(true);
                    if (!toDistribute.isEmpty() && partAt != be) {
                        GasStack copy = toDistribute.copy();
                        IGasTank tank = partAt instanceof iGas iGasPart ? iGasPart.getTank(0) : null;
                        long split = Math.min(maxCapacity, toDistribute.getAmount());
                        copy.setAmount(split);
                        toDistribute.shrink(split);
                        if (tank != null) {
                            tank.fill(copy, GasAction.EXECUTE);
                        }
                    }

                    if (cache == null) {
                        continue;
                    }

                    cache.put(pos, partAt);
                }
            }
        }

        if (be instanceof IGasTankMultiBlockEntityContainer.Inventory inv && inv.hasInventory() && be.getLevel() != null) {
            be.getLevel().invalidateCapabilities(be.getBlockPos());
        }
        if (be instanceof iGas iGas && iGas.hasTank() && be.getLevel() != null) {
            be.getLevel().invalidateCapabilities(be.getBlockPos());
        }
    }

    @Nullable
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> T partAt(BlockEntityType<?> type, @NotNull BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getType() == type && !be.isRemoved() ? checked(be) : null;
    }

    private static class SearchCache<T extends BlockEntity & IGasTankMultiBlockEntityContainer> {
        Map<BlockPos, Optional<T>> controllerMap;

        public SearchCache() {
            controllerMap = new HashMap<>();
        }

        Optional<T> getOrCache(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
            if (hasVisited(pos)) {
                return controllerMap.get(pos);
            }

            T partAt = partAt(type, level, pos);
            if (partAt == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            T controller = checked(level.getBlockEntity(partAt.getController()));
            if (controller == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            put(pos, controller);
            return Optional.of(controller);
        }

        void put(BlockPos pos, T target) {
            controllerMap.put(pos, Optional.of(target));
        }

        void putEmpty(BlockPos pos) {
            controllerMap.put(pos, Optional.empty());
        }

        boolean hasVisited(BlockPos pos) {
            return controllerMap.containsKey(pos);
        }
    }
}
