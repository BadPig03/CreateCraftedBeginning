package net.ty.createcraftedbeginning.api.gas.gases;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTankMultiBlockEntityContainer;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasConnectivityHandler {
    /**
     * Forms or rebuilds the multiblock structure around the supplied block entity.
     *
     * @param <T>   the value type constrained by {@code extends BlockEntity & IGasTankMultiBlockEntityContainer}
     * @param be    the block entity that participates in the operation
     * @param level the level in which the operation is performed
     */
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void formMulti(T be, Level level) {
        SearchCache<T> cache = new SearchCache<>();
        List<T> frontier = new ArrayList<>();
        frontier.add(be);
        formMulti(be.getType(), level, cache, frontier);
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void formMulti(BlockEntityType<?> type, BlockGetter level, SearchCache<T> cache, List<T> frontier) {
        PriorityQueue<Pair<Integer, T>> creationQueue = makeCreationQueue();
        Set<BlockPos> visited = new HashSet<>();
        Axis mainAxis = frontier.getFirst().getMainConnectionAxis();
        int minX = mainAxis == Axis.X ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minY = mainAxis == Axis.Y ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minZ = mainAxis == Axis.Z ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (T be : frontier) {
            BlockPos pos = be.getBlockPos();
            minX = Math.min(pos.getX(), minX);
            minY = Math.min(pos.getY(), minY);
            minZ = Math.min(pos.getZ(), minZ);
        }

        int maxWidth = frontier.getFirst().getMaxWidth();
        if (mainAxis != Axis.X) {
            minX -= maxWidth;
        }
        if (mainAxis != Axis.Y) {
            minY -= maxWidth;
        }
        if (mainAxis != Axis.Z) {
            minZ -= maxWidth;
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

                T nextPart = partAt(type, level, next);
                if (nextPart == null || nextPart.isRemoved()) {
                    continue;
                }

                frontier.add(nextPart);
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

    /**
     * Checks whether the supplied positions belong to the same connected structure.
     *
     * @param <T>   the value type constrained by {@code extends BlockEntity & IGasTankMultiBlockEntityContainer}
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @param other the object to compare with this instance
     * @return {@code true} if the supplied positions belong to the same connected structure; otherwise {@code
     * false}
     */
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> boolean isConnected(BlockGetter level, BlockPos pos, BlockPos other) {
        T one = checked(level.getBlockEntity(pos));
        T two = checked(level.getBlockEntity(other));
        return one != null && two != null && one.getController().equals(two.getController());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> T checked(@Nullable BlockEntity be) {
        return be instanceof IGasTankMultiBlockEntityContainer ? (T) be : null;
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> int tryToFormNewMulti(T be, SearchCache<T> cache, boolean simulate) {
        if (!be.isController()) {
            return 0;
        }

        int bestWidth = 1;
        int bestAmount = -1;
        int radius = be.getMaxWidth();
        for (int width = 1; width <= radius; width++) {
            int amount = tryToFormNewMultiOfWidth(be, width, cache, true);
            if (amount < bestAmount) {
                continue;
            }

            bestWidth = width;
            bestAmount = amount;
        }

        if (simulate) {
            return bestAmount;
        }

        int currentWidth = be.getWidth();
        if (currentWidth == bestWidth && currentWidth * currentWidth * be.getHeight() == bestAmount) {
            return bestAmount;
        }

        splitMultiAndInvalidate(be, cache);
        if (be.hasTank()) {
            be.setTankSize(0, bestAmount);
        }

        tryToFormNewMultiOfWidth(be, bestWidth, cache, false);
        be.preventConnectivityUpdate();
        be.setWidth(bestWidth);
        be.setHeight(bestAmount / bestWidth / bestWidth);
        be.notifyMultiUpdated();
        return bestAmount;
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> int tryToFormNewMultiOfWidth(T be, int width, SearchCache<T> cache, boolean simulate) {
        Level level = be.getLevel();
        if (level == null) {
            return 0;
        }

        int amount = 0;
        int height = 0;
        BlockEntityType<?> type = be.getType();
        BlockPos origin = be.getBlockPos();
        IGasTank tank = null;
        GasStack gas = GasStack.EMPTY;
        if (be.hasTank()) {
            tank = be.getTank(0);
            gas = tank.getGasStack();
        }

        Axis axis = be.getMainConnectionAxis();
        int maxLength = be.getMaxLength(axis, width);
        Search:
        for (int lengthOffset = 0; lengthOffset < maxLength; lengthOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = getPartPos(origin, axis, lengthOffset, xOffset, zOffset);
                    Optional<@NotNull T> cachedPart = cache.getOrCache(type, level, pos);
                    if (cachedPart.isEmpty()) {
                        break Search;
                    }

                    T controller = cachedPart.get();
                    int otherWidth = controller.getWidth();
                    if (otherWidth > width || otherWidth == width && controller.getHeight() == maxLength) {
                        break Search;
                    }
                    if (axis != controller.getMainConnectionAxis()) {
                        break Search;
                    }

                    BlockPos controllerPos = controller.getBlockPos();
                    if (!controllerPos.equals(origin) && isOutsideFormationBounds(axis, origin, controllerPos, otherWidth, width)) {
                        break Search;
                    }

                    if (controller.hasTank()) {
                        GasStack otherGas = controller.getGas(0);
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
        for (int lengthOffset = 0; lengthOffset < height; lengthOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = getPartPos(origin, axis, lengthOffset, xOffset, zOffset);
                    T part = partAt(type, level, pos);
                    if (part == null || part == be) {
                        continue;
                    }

                    extraData = be.modifyExtraData(extraData);
                    if (part.hasTank()) {
                        IGasTank partTank = part.getTank(0);
                        GasStack partGas = partTank.getGasStack();
                        if (!partGas.isEmpty() && be.hasTank() && tank != null) {
                            tank.fill(partGas, GasAction.EXECUTE);
                        }
                        partTank.drain(partTank.getCapacity(), GasAction.EXECUTE);
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

    private static BlockPos getPartPos(BlockPos origin, Axis axis, int lengthOffset, int xOffset, int zOffset) {
        return switch (axis) {
            case X -> origin.offset(lengthOffset, xOffset, zOffset);
            case Y -> origin.offset(xOffset, lengthOffset, zOffset);
            case Z -> origin.offset(xOffset, zOffset, lengthOffset);
        };
    }

    private static boolean isOutsideFormationBounds(Axis axis, BlockPos origin, BlockPos controllerPos, int controllerWidth, int width) {
        if (axis == Axis.Y) {
            return controllerPos.getX() < origin.getX() || controllerPos.getZ() < origin.getZ() || controllerPos.getX() + controllerWidth > origin.getX() + width || controllerPos.getZ() + controllerWidth > origin.getZ() + width;
        }

        if (controllerPos.getY() < origin.getY() || controllerPos.getY() + controllerWidth > origin.getY() + width) {
            return true;
        }

        if (axis == Axis.Z) {
            return controllerPos.getX() < origin.getX() || controllerPos.getX() + controllerWidth > origin.getX() + width;
        }

        return controllerPos.getZ() < origin.getZ() || controllerPos.getZ() + controllerWidth > origin.getZ() + width;
    }

    @Contract(value = " -> new", pure = true)
    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> @NotNull PriorityQueue<Pair<Integer, T>> makeCreationQueue() {
        return new PriorityQueue<>((one, two) -> two.getKey() - one.getKey());
    }

    /**
     * Splits the multiblock structure containing the supplied block entity.
     *
     * @param <T> the value type constrained by {@code extends BlockEntity & IGasTankMultiBlockEntityContainer}
     * @param be  the block entity that participates in the operation
     */
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void splitMulti(T be) {
        splitMultiAndInvalidate(be, null);
    }

    private static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void splitMultiAndInvalidate(T be, @Nullable SearchCache<T> cache) {
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
        if (be.hasTank()) {
            toDistribute = be.getGas(0);
            maxCapacity = be.getTankSize(0);
            if (!toDistribute.isEmpty() && !be.isRemoved()) {
                toDistribute.shrink(maxCapacity);
            }
            be.setTankSize(0, 1);
        }

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };

                    T part = partAt(be.getType(), level, pos);
                    if (part == null || !part.getController().equals(origin)) {
                        continue;
                    }

                    T controller = part.getControllerBE();
                    part.setExtraData(controller == null ? null : controller.getExtraData());
                    part.removeController(true);
                    if (!toDistribute.isEmpty() && part != be) {
                        GasStack copy = toDistribute.copy();
                        IGasTank tank = part.getTank(0);
                        long split = Math.min(maxCapacity, toDistribute.getAmount());
                        copy.setAmount(split);
                        toDistribute.shrink(split);
                        tank.fill(copy, GasAction.EXECUTE);
                    }

                    if (cache != null) {
                        cache.put(pos, part);
                    }
                }
            }
        }

        if (be instanceof IGasTankMultiBlockEntityContainer.Inventory inv && inv.hasInventory() && be.getLevel() != null) {
            be.getLevel().invalidateCapabilities(be.getBlockPos());
        }
        if (be.hasTank() && be.getLevel() != null) {
            be.getLevel().invalidateCapabilities(be.getBlockPos());
        }
    }

    /**
     * Returns the compatible multiblock part at the supplied position.
     *
     * @param <T>   the value type constrained by {@code extends BlockEntity & IGasTankMultiBlockEntityContainer}
     * @param type  the type to use
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @return the resulting value
     */
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> @Nullable T partAt(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getType() == type && !be.isRemoved() ? checked(be) : null;
    }

    private static class SearchCache<T extends BlockEntity & IGasTankMultiBlockEntityContainer> {
        protected Map<BlockPos, Optional<@NotNull T>> controllerMap;

        /**
         * Creates a new {@code SearchCache} instance.
         */
        public SearchCache() {
            controllerMap = new HashMap<>();
        }

        protected Optional<@NotNull T> getOrCache(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
            if (hasVisited(pos)) {
                return controllerMap.get(pos);
            }

            T partAt = partAt(type, level, pos);
            if (partAt == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            T controller = partAt(type, level, partAt.getController());
            if (controller == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            put(pos, controller);
            return Optional.of(controller);
        }

        protected void put(BlockPos pos, T target) {
            controllerMap.put(pos, Optional.of(target));
        }

        protected void putEmpty(BlockPos pos) {
            controllerMap.put(pos, Optional.empty());
        }

        protected boolean hasVisited(BlockPos pos) {
            return controllerMap.containsKey(pos);
        }
    }
}
