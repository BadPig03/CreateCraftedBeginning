package net.ty.createcraftedbeginning.content.airtights.gas.transport;

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
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTankMultiBlockEntityContainer;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasConnectivityHandler {
    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void formMulti(T be, Level level) {
        SearchCache cache = new SearchCache();
        Deque<IGasTankMultiBlockEntityContainer> frontier = new ArrayDeque<>();
        frontier.addLast(be);
        formMulti(be.getType(), level, cache, frontier);
    }

    private static void formMulti(BlockEntityType<?> type, BlockGetter level, SearchCache cache, Deque<IGasTankMultiBlockEntityContainer> frontier) {
        PriorityQueue<Pair<Integer, IGasTankMultiBlockEntityContainer>> creationQueue = makeCreationQueue();
        Set<BlockPos> visited = new HashSet<>();
        Axis mainAxis = frontier.getFirst().getMainConnectionAxis();
        int minX = mainAxis == Axis.X ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minY = mainAxis == Axis.Y ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minZ = mainAxis == Axis.Z ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (IGasTankMultiBlockEntityContainer be : frontier) {
            BlockPos pos = blockEntity(be).getBlockPos();
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
            IGasTankMultiBlockEntityContainer part = frontier.removeFirst();
            BlockPos partPos = blockEntity(part).getBlockPos();
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

                IGasTankMultiBlockEntityContainer nextPart = tankAt(type, level, next);
                if (nextPart == null) {
                    continue;
                }

                frontier.addLast(nextPart);
            }
        }

        visited.clear();
        while (!creationQueue.isEmpty()) {
            Pair<Integer, IGasTankMultiBlockEntityContainer> next = creationQueue.poll();
            IGasTankMultiBlockEntityContainer toCreate = next.getValue();
            if (visited.contains(blockEntity(toCreate).getBlockPos())) {
                continue;
            }

            visited.add(blockEntity(toCreate).getBlockPos());
            tryToFormNewMulti(toCreate, cache, false);
        }
    }

    public static boolean isConnected(BlockGetter level, BlockPos pos, BlockPos other) {
        BlockEntity first = level.getBlockEntity(pos);
        BlockEntity second = level.getBlockEntity(other);
        return first instanceof IGasTankMultiBlockEntityContainer one && second instanceof IGasTankMultiBlockEntityContainer two && one.getController().equals(two.getController());
    }

    private static int tryToFormNewMulti(IGasTankMultiBlockEntityContainer be, SearchCache cache, boolean simulate) {
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

    private static int tryToFormNewMultiOfWidth(IGasTankMultiBlockEntityContainer be, int width, SearchCache cache, boolean simulate) {
        Level level = blockEntity(be).getLevel();
        if (level == null) {
            return 0;
        }

        int amount = 0;
        int height = 0;
        BlockEntityType<?> type = blockEntity(be).getType();
        BlockPos origin = blockEntity(be).getBlockPos();
        GasStack gas = GasStack.EMPTY;
        if (be.hasTank()) {
            gas = be.getGas(0);
        }

        Axis axis = be.getMainConnectionAxis();
        int maxLength = be.getMaxLength(axis, width);
        Search:
        for (int lengthOffset = 0; lengthOffset < maxLength; lengthOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = getPartPos(origin, axis, lengthOffset, xOffset, zOffset);
                    Optional<@NotNull IGasTankMultiBlockEntityContainer> cachedPart = cache.getOrCache(type, level, pos);
                    if (cachedPart.isEmpty()) {
                        break Search;
                    }

                    IGasTankMultiBlockEntityContainer controller = cachedPart.get();
                    int otherWidth = controller.getWidth();
                    if (otherWidth > width || otherWidth == width && controller.getHeight() == maxLength) {
                        break Search;
                    }
                    if (axis != controller.getMainConnectionAxis()) {
                        break Search;
                    }

                    BlockPos controllerPos = blockEntity(controller).getBlockPos();
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
                    IGasTankMultiBlockEntityContainer part = tankAt(type, level, pos);
                    if (part == null || part == be) {
                        continue;
                    }

                    extraData = be.modifyExtraData(extraData);
                    be.mergeTankStateFrom(part);

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
    private static PriorityQueue<Pair<Integer, IGasTankMultiBlockEntityContainer>> makeCreationQueue() {
        return new PriorityQueue<>((one, two) -> two.getKey() - one.getKey());
    }

    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> void splitMulti(T be) {
        splitMultiAndInvalidate(be, null);
    }

    private static void splitMultiAndInvalidate(IGasTankMultiBlockEntityContainer be, @Nullable SearchCache cache) {
        Level level = blockEntity(be).getLevel();
        if (level == null) {
            return;
        }

        be = tankAt(blockEntity(be).getType(), level, be.getController());
        if (be == null) {
            return;
        }

        int height = be.getHeight();
        int width = be.getWidth();
        if (width == 1 && height == 1) {
            return;
        }

        BlockPos origin = blockEntity(be).getBlockPos();
        Axis axis = be.getMainConnectionAxis();
        GasStack toDistribute = GasStack.EMPTY;
        if (be.hasTank()) {
            toDistribute = be.prepareTankStateForSplit(0, blockEntity(be).isRemoved());
        }

        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };

                    IGasTankMultiBlockEntityContainer part = tankAt(blockEntity(be).getType(), level, pos);
                    if (part == null || !part.getController().equals(origin)) {
                        continue;
                    }

                    IGasTankMultiBlockEntityContainer controller = tankAt(blockEntity(part).getType(), level, part.getController());
                    part.setExtraData(controller == null ? null : controller.getExtraData());
                    part.removeController(true);
                    if (!toDistribute.isEmpty() && part != be) {
                        part.applySplitTankState(0, toDistribute);
                    }

                    if (cache != null) {
                        cache.put(pos, part);
                    }
                }
            }
        }

        if (be instanceof IGasTankMultiBlockEntityContainer.Inventory inv && inv.hasInventory()) {
            level.invalidateCapabilities(blockEntity(be).getBlockPos());
        }
        if (!be.hasTank()) {
            return;
        }

        level.invalidateCapabilities(blockEntity(be).getBlockPos());
    }

    public static <T extends BlockEntity & IGasTankMultiBlockEntityContainer> @Nullable T partAt(BlockEntityType<T> type, BlockGetter level, BlockPos pos) {
        T blockEntity = type.getBlockEntity(level, pos);
        return blockEntity != null && !blockEntity.isRemoved() ? blockEntity : null;
    }

    private static @Nullable IGasTankMultiBlockEntityContainer tankAt(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.getType() != type || blockEntity.isRemoved() || !(blockEntity instanceof IGasTankMultiBlockEntityContainer tank)) {
            return null;
        }
        return tank;
    }

    private static BlockEntity blockEntity(IGasTankMultiBlockEntityContainer tank) {
        if (tank instanceof BlockEntity blockEntity) {
            return blockEntity;
        }

        throw new IllegalStateException("Gas multiblock container is not a block entity");
    }

    private static class SearchCache {
        protected Map<BlockPos, Optional<@NotNull IGasTankMultiBlockEntityContainer>> controllerMap;

        protected SearchCache() {
            controllerMap = new HashMap<>();
        }

        protected Optional<@NotNull IGasTankMultiBlockEntityContainer> getOrCache(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
            if (hasVisited(pos)) {
                return controllerMap.get(pos);
            }

            IGasTankMultiBlockEntityContainer partAt = tankAt(type, level, pos);
            if (partAt == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            IGasTankMultiBlockEntityContainer controller = tankAt(type, level, partAt.getController());
            if (controller == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            put(pos, controller);
            return Optional.of(controller);
        }

        protected void put(BlockPos pos, IGasTankMultiBlockEntityContainer target) {
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
