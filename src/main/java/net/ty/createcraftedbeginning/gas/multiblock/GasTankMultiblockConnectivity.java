package net.ty.createcraftedbeginning.gas.multiblock;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer.Inventory;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasTankMultiblockConnectivity {
    private GasTankMultiblockConnectivity() {
    }

    public static boolean isConnected(BlockGetter level, BlockPos pos, BlockPos other) {
        BlockEntity firstEntity = level.getBlockEntity(pos);
        BlockEntity secondEntity = level.getBlockEntity(other);
        return firstEntity instanceof GasTankMultiblockPart firstTank && secondEntity instanceof GasTankMultiblockPart secondTank && firstTank.getController().equals(secondTank.getController());
    }

    public static <T extends BlockEntity & GasTankMultiblockPart> void splitMultiblockOnRemoval(T blockEntity, boolean releaseRemovedTankGas) {
        splitMultiblock(blockEntity, null, blockEntity.getBlockPos(), releaseRemovedTankGas);
    }

    public static <T extends BlockEntity & GasTankMultiblockPart> void formMultiblock(T blockEntity, Level level) {
        Deque<GasTankMultiblockPart> frontier = new ArrayDeque<>();
        frontier.addLast(blockEntity);
        formMultiblock(blockEntity.getType(), level, new PartLookupCache(), frontier);
    }

    public static <T extends BlockEntity & GasTankMultiblockPart> @Nullable T partAt(BlockEntityType<T> type, BlockGetter level, BlockPos pos) {
        T blockEntity = type.getBlockEntity(level, pos);
        if (blockEntity == null || blockEntity.isRemoved()) {
            return null;
        }

        return blockEntity;
    }

    private static void formMultiblock(BlockEntityType<?> type, BlockGetter level, PartLookupCache cache, Deque<GasTankMultiblockPart> frontier) {
        PriorityQueue<Pair<Integer, GasTankMultiblockPart>> creationQueue = new PriorityQueue<>((firstEntry, secondEntry) -> secondEntry.getKey() - firstEntry.getKey());
        Set<BlockPos> visited = new HashSet<>();
        Axis mainAxis = frontier.getFirst().getMainConnectionAxis();
        int minX = mainAxis == Axis.X ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minY = mainAxis == Axis.Y ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int minZ = mainAxis == Axis.Z ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (GasTankMultiblockPart part : frontier) {
            BlockPos partPos = asBlockEntity(part).getBlockPos();
            minX = Math.min(partPos.getX(), minX);
            minY = Math.min(partPos.getY(), minY);
            minZ = Math.min(partPos.getZ(), minZ);
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
            GasTankMultiblockPart part = frontier.removeFirst();
            BlockPos partPos = asBlockEntity(part).getBlockPos();
            if (visited.contains(partPos)) {
                continue;
            }

            visited.add(partPos);
            int partCount = tryFormMultiblock(part, cache, true);
            if (partCount > 1) {
                creationQueue.add(Pair.of(partCount, part));
            }

            for (Axis axis : Iterate.axes) {
                Direction negativeDirection = Direction.get(AxisDirection.NEGATIVE, axis);
                BlockPos neighborPos = partPos.relative(negativeDirection);
                if (neighborPos.getX() <= minX || neighborPos.getY() <= minY || neighborPos.getZ() <= minZ) {
                    continue;
                }

                if (visited.contains(neighborPos)) {
                    continue;
                }

                GasTankMultiblockPart neighborPart = resolvePart(type, level, neighborPos);
                if (neighborPart == null) {
                    continue;
                }

                frontier.addLast(neighborPart);
            }
        }

        visited.clear();
        while (!creationQueue.isEmpty()) {
            Pair<Integer, GasTankMultiblockPart> candidate = creationQueue.poll();
            GasTankMultiblockPart controller = candidate.getValue();
            if (visited.contains(asBlockEntity(controller).getBlockPos())) {
                continue;
            }

            visited.add(asBlockEntity(controller).getBlockPos());
            tryFormMultiblock(controller, cache, false);
        }
    }

    private static int tryFormMultiblock(GasTankMultiblockPart tankPart, PartLookupCache cache, boolean simulate) {
        if (!tankPart.isController()) {
            return 0;
        }

        int bestWidth = 1;
        int bestPartCount = -1;
        int maxWidth = tankPart.getMaxWidth();
        for (int width = 1; width <= maxWidth; width++) {
            int partCount = tryFormMultiblockOfWidth(tankPart, width, cache, true);
            if (partCount < bestPartCount) {
                continue;
            }

            bestWidth = width;
            bestPartCount = partCount;
        }

        if (simulate) {
            return bestPartCount;
        }

        int currentWidth = tankPart.getWidth();
        if (currentWidth == bestWidth && Mth.square(currentWidth) * tankPart.getHeight() == bestPartCount) {
            return bestPartCount;
        }

        splitMultiblock(tankPart, cache, null, false);
        if (tankPart.hasTank()) {
            tankPart.setTankBlockCount(0, bestPartCount);
        }

        tryFormMultiblockOfWidth(tankPart, bestWidth, cache, false);
        tankPart.preventConnectivityUpdate();
        tankPart.setWidth(bestWidth);
        tankPart.setHeight(bestPartCount / bestWidth / bestWidth);
        tankPart.notifyMultiUpdated();
        return bestPartCount;
    }

    private static int tryFormMultiblockOfWidth(GasTankMultiblockPart tankPart, int width, PartLookupCache cache, boolean simulate) {
        Level level = asBlockEntity(tankPart).getLevel();
        if (level == null) {
            return 0;
        }

        int partCount = 0;
        int height = 0;
        BlockEntityType<?> type = asBlockEntity(tankPart).getType();
        BlockPos origin = asBlockEntity(tankPart).getBlockPos();
        GasStack formationGas = GasStack.EMPTY;
        if (tankPart.hasTank()) {
            formationGas = tankPart.getGas(0);
        }

        Axis axis = tankPart.getMainConnectionAxis();
        int maxLength = tankPart.getMaxLength(axis, width);
        Search:
        for (int lengthOffset = 0; lengthOffset < maxLength; lengthOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos partPos = getPartPos(origin, axis, lengthOffset, xOffset, zOffset);
                    Optional<@NotNull GasTankMultiblockPart> cachedPart = cache.getController(type, level, partPos);
                    if (cachedPart.isEmpty()) {
                        break Search;
                    }

                    GasTankMultiblockPart controller = cachedPart.get();
                    int controllerWidth = controller.getWidth();
                    if (controllerWidth > width || axis != controller.getMainConnectionAxis() || controllerWidth == width && controller.getHeight() == maxLength) {
                        break Search;
                    }

                    BlockPos controllerPos = asBlockEntity(controller).getBlockPos();
                    if (!controllerPos.equals(origin) && isOutsideFormationBounds(axis, origin, controllerPos, controllerWidth, width)) {
                        break Search;
                    }

                    if (!controller.hasTank()) {
                        continue;
                    }

                    GasStack controllerGas = controller.getGas(0);
                    if (controllerGas.isEmpty()) {
                        continue;
                    }

                    if (formationGas.isEmpty()) {
                        formationGas = controllerGas.copy();
                        continue;
                    }

                    if (!GasStack.isSameGasSameComponents(formationGas, controllerGas)) {
                        break Search;
                    }
                }
            }
            partCount += Mth.square(width);
            height++;
        }

        if (simulate) {
            return partCount;
        }

        Object extraData = tankPart.getExtraData();
        for (int lengthOffset = 0; lengthOffset < height; lengthOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos partPos = getPartPos(origin, axis, lengthOffset, xOffset, zOffset);
                    GasTankMultiblockPart part = resolvePart(type, level, partPos);
                    if (part == null || part == tankPart) {
                        continue;
                    }

                    extraData = tankPart.modifyExtraData(extraData);
                    splitMultiblock(part, cache, null, false);
                    mergeOverpressureStress(tankPart, part);
                    tankPart.mergeTankStateFrom(part);
                    part.setController(origin);
                    part.preventConnectivityUpdate();
                    cache.put(partPos, tankPart);
                    part.setHeight(height);
                    part.setWidth(width);
                    part.notifyMultiUpdated();
                }
            }
        }

        propagateOverpressureStress(tankPart, type, level, origin, axis, width, height);
        tankPart.setExtraData(extraData);
        return partCount;
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

    private static void splitMultiblock(GasTankMultiblockPart tankPart, @Nullable PartLookupCache cache, @Nullable BlockPos removedPos, boolean releaseRemovedTankGas) {
        Level level = asBlockEntity(tankPart).getLevel();
        if (level == null) {
            return;
        }

        boolean tankPhysicallyRemoved = releaseRemovedTankGas && removedPos != null && removedPos.equals(asBlockEntity(tankPart).getBlockPos()) && tankPart.hasTank();
        tankPart = resolvePart(asBlockEntity(tankPart).getType(), level, tankPart.getController());
        if (tankPart == null) {
            return;
        }

        int height = tankPart.getHeight();
        int width = tankPart.getWidth();
        if (width == 1 && height == 1) {
            return;
        }

        double splitOverpressureStress = getOverpressureStress(tankPart);
        BlockPos origin = asBlockEntity(tankPart).getBlockPos();
        Axis axis = tankPart.getMainConnectionAxis();
        boolean controllerRemoved = origin.equals(removedPos);
        List<GasTankMultiblockPart> remainingParts = new ArrayList<>(Mth.square(width) * height);
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos partPos = switch (axis) {
                        case X -> origin.offset(yOffset, xOffset, zOffset);
                        case Y -> origin.offset(xOffset, yOffset, zOffset);
                        case Z -> origin.offset(xOffset, zOffset, yOffset);
                    };

                    if (partPos.equals(removedPos)) {
                        if (cache != null) {
                            cache.putEmpty(partPos);
                        }
                        continue;
                    }

                    GasTankMultiblockPart part = resolvePart(asBlockEntity(tankPart).getType(), level, partPos);
                    if (part == null || !part.getController().equals(origin)) {
                        continue;
                    }

                    remainingParts.add(part);
                }
            }
        }

        int remainingTankCount = 0;
        for (GasTankMultiblockPart part : remainingParts) {
            if (!part.hasTank()) {
                continue;
            }

            remainingTankCount++;
        }

        GasStack splitGas = GasStack.EMPTY;
        if (tankPart.hasTank()) {
            GasPressureCompartment tank = tankPart.getTank(0);
            boolean releaseRemovedGas = tankPhysicallyRemoved && tank.getPressureModel() == PressureModel.VARIABLE;
            long sourcePressurePa = tank.getPressurePa();
            splitGas = tankPart.prepareTankStateForSplit(0, controllerRemoved);
            if (releaseRemovedGas && removedPos != null) {
                GasStack releasedGas = extractRemovedTankShare(splitGas, remainingTankCount);
                releaseRemovedTankGas(level, removedPos, releasedGas, sourcePressurePa);
            }
        }

        for (GasTankMultiblockPart part : remainingParts) {
            BlockPos partPos = asBlockEntity(part).getBlockPos();
            GasTankMultiblockPart partController = resolvePart(asBlockEntity(part).getType(), level, part.getController());
            part.setExtraData(partController == null ? null : partController.getExtraData());
            part.removeController(true);
            if (part.hasTank()) {
                part.applySplitTankState(0, splitGas, remainingTankCount);
                remainingTankCount--;
            }
            setOverpressureStress(part, splitOverpressureStress);

            if (cache == null) {
                continue;
            }

            cache.put(partPos, part);
        }

        if (tankPart instanceof Inventory inv && inv.hasInventory()) {
            level.invalidateCapabilities(asBlockEntity(tankPart).getBlockPos());
        }
        if (!tankPart.hasTank()) {
            return;
        }

        level.invalidateCapabilities(asBlockEntity(tankPart).getBlockPos());
    }

    private static void mergeOverpressureStress(GasTankMultiblockPart target, GasTankMultiblockPart source) {
        if (!(target instanceof OverpressureMultiblockPart targetPressure) || !(source instanceof OverpressureMultiblockPart sourcePressure)) {
            return;
        }

        targetPressure.setMultiblockOverpressureStress(Math.max(targetPressure.getMultiblockOverpressureStress(), sourcePressure.getMultiblockOverpressureStress()));
    }

    private static void propagateOverpressureStress(GasTankMultiblockPart controller, BlockEntityType<?> type, Level level, BlockPos origin, Axis axis, int width, int height) {
        if (!(controller instanceof OverpressureMultiblockPart controllerPressure)) {
            return;
        }

        double stress = controllerPressure.getMultiblockOverpressureStress();
        for (int lengthOffset = 0; lengthOffset < height; lengthOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    GasTankMultiblockPart part = resolvePart(type, level, getPartPos(origin, axis, lengthOffset, xOffset, zOffset));
                    setOverpressureStress(part, stress);
                }
            }
        }
    }

    private static double getOverpressureStress(GasTankMultiblockPart part) {
        if (!(part instanceof OverpressureMultiblockPart pressurePart)) {
            return 0;
        }

        return pressurePart.getMultiblockOverpressureStress();
    }

    private static void setOverpressureStress(@Nullable GasTankMultiblockPart part, double stress) {
        if (!(part instanceof OverpressureMultiblockPart pressurePart)) {
            return;
        }

        pressurePart.setMultiblockOverpressureStress(stress);
    }

    private static GasStack extractRemovedTankShare(GasStack splitGas, int remainingTankCount) {
        if (splitGas.isEmpty()) {
            return GasStack.EMPTY;
        }

        int originalTankCount = remainingTankCount + 1;
        long amount = splitGas.getAmount();
        long releasedAmount = originalTankCount <= 1 ? amount : amount / originalTankCount;
        if (originalTankCount > 1 && amount % originalTankCount != 0) {
            releasedAmount++;
        }

        GasStack releasedGas = splitGas.copyWithAmount(releasedAmount);
        splitGas.shrink(releasedAmount);
        return releasedGas;
    }

    private static void releaseRemovedTankGas(Level level, BlockPos removedPos, GasStack releasedGas, long sourcePressurePa) {
        if (level.isClientSide || releasedGas.isEmpty()) {
            return;
        }

        GasReleaseService.release(level, GasReleaseRequest.radial(releasedGas, removedPos, GasReleaseCause.TANK_REMOVAL, sourcePressurePa));
    }

    private static @Nullable GasTankMultiblockPart resolvePart(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || blockEntity.getType() != type || blockEntity.isRemoved() || !(blockEntity instanceof GasTankMultiblockPart tank)) {
            return null;
        }

        return tank;
    }

    private static BlockEntity asBlockEntity(GasTankMultiblockPart tank) {
        if (!(tank instanceof BlockEntity blockEntity)) {
            throw new IllegalStateException("Gas multiblock container is not a block entity.");
        }

        return blockEntity;
    }

    private static class PartLookupCache {
        private final Map<BlockPos, Optional<@NotNull GasTankMultiblockPart>> controllersByPosition;

        private PartLookupCache() {
            controllersByPosition = new HashMap<>();
        }

        private Optional<@NotNull GasTankMultiblockPart> getController(BlockEntityType<?> type, BlockGetter level, BlockPos pos) {
            if (contains(pos)) {
                return controllersByPosition.get(pos);
            }

            GasTankMultiblockPart part = resolvePart(type, level, pos);
            if (part == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            GasTankMultiblockPart controller = resolvePart(type, level, part.getController());
            if (controller == null) {
                putEmpty(pos);
                return Optional.empty();
            }

            put(pos, controller);
            return Optional.of(controller);
        }

        private void put(BlockPos pos, GasTankMultiblockPart controller) {
            controllersByPosition.put(pos, Optional.of(controller));
        }

        private void putEmpty(BlockPos pos) {
            controllersByPosition.put(pos, Optional.empty());
        }

        private boolean contains(BlockPos pos) {
            return controllersByPosition.containsKey(pos);
        }
    }
}
