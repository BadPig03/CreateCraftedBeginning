package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Unmodifiable;
import snownee.jade.api.ITooltip;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.ViewGroup;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStorageDataProvider {
    public static final String STORAGE_KEY = "JadeGasStorage";
    public static final String STORAGE_UID_KEY = "JadeGasStorageUid";

    private static final String COMPOUND_KEY_HIDDEN_TANK_COUNT = "+";
    private static final ResourceLocation ICON = CCBAPI.asResource("icon");

    public static @Unmodifiable List<ViewGroup<CompoundTag>> fromGasHandler(GasHandler gasHandler, boolean creative, boolean includeCapacity, boolean includePressure) {
        StorageCollection storageCollection = collectStorage(gasHandler, includeCapacity, includePressure);
        if (storageCollection.tankCount == 0) {
            return List.of();
        }

        int maxDisplayedEntries = storageCollection.emptyTankCount == 0 ? 5 : 4;
        List<StorageEntryData> displayedEntries = new ArrayList<>(storageCollection.entries.limit(maxDisplayedEntries).toList());
        int hiddenTankCount = storageCollection.tankCount - storageCollection.emptyTankCount - displayedEntries.size();
        if (storageCollection.emptyTankCount > 0) {
            displayedEntries.add(new StorageEntryData(GasStorageEntry.empty(), storageCollection.emptyMaxAmount, storageCollection.emptyPressurePa));
        }

        List<CompoundTag> serializedViews = displayedEntries.stream().map(entry -> GasStorageView.writeObserved(entry.entry(), entry.maxAmount(), creative, entry.pressurePa(), includeCapacity, includePressure)).toList();
        ViewGroup<CompoundTag> group = new ViewGroup<>(serializedViews);
        if (hiddenTankCount <= 0) {
            return List.of(group);
        }

        group.getExtraData().putInt(COMPOUND_KEY_HIDDEN_TANK_COUNT, hiddenTankCount);
        return List.of(group);
    }

    public static void readData(CompoundTag data, Set<GasHandler> gasHandlers, ResourceLocation location, boolean creative) {
        readData(data, gasHandlers, location, creative, false, false);
    }

    public static void readData(CompoundTag data, Set<GasHandler> gasHandlers, ResourceLocation location, boolean creative, boolean includeCapacity, boolean includePressure) {
        List<ViewGroup<CompoundTag>> groups = new ArrayList<>();
        for (GasHandler gasHandler : gasHandlers) {
            groups.addAll(fromGasHandler(gasHandler, creative, includeCapacity, includePressure));
        }
        ViewGroup.saveList(data, STORAGE_KEY, groups, Function.identity());
        data.putString(STORAGE_UID_KEY, location.toString());
    }

    public static void appendData(ITooltip tooltip, CompoundTag data, boolean showDetails, boolean includeCapacity, boolean includePressure) {
        List<ViewGroup<CompoundTag>> groups;
        try {
            groups = ViewGroup.readList(data, STORAGE_KEY, Function.identity());
        }
        catch (Exception exception) {
            CCBAPI.LOGGER.error("Failed to read Jade gas storage tooltip data.", exception);
            return;
        }

        if (groups == null || groups.isEmpty()) {
            return;
        }

        List<ClientViewGroup<GasStorageView>> clientGroups = new ArrayList<>();
        for (ViewGroup<CompoundTag> group : groups) {
            List<GasStorageView> views = group.views.stream().map(tag -> GasStorageView.readDefault(tag, includeCapacity, includePressure)).filter(Objects::nonNull).collect(Collectors.toList());
            if (views.isEmpty()) {
                continue;
            }

            ClientViewGroup<GasStorageView> clientGroup = new ClientViewGroup<>(views);
            clientGroup.extraData = group.getExtraData().copy();
            clientGroups.add(clientGroup);
        }
        if (clientGroups.isEmpty()) {
            return;
        }

        IElementHelper elementHelper = IElementHelper.get();
        boolean renderGroup = clientGroups.size() > 1 || clientGroups.getFirst().shouldRenderGroup();
        ClientViewGroup.tooltip(tooltip, clientGroups, renderGroup, (groupTooltip, group) -> {
            if (renderGroup && group.shouldRenderGroup()) {
                group.renderHeader(groupTooltip);
            }
            for (GasStorageView view : group.views) {
                appendView(groupTooltip, view, showDetails, includePressure, elementHelper);
            }
            int hiddenTankCount = 0;
            CompoundTag extraData = group.extraData;
            if (extraData != null) {
                hiddenTankCount = extraData.getInt(COMPOUND_KEY_HIDDEN_TANK_COUNT);
            }
            if (hiddenTankCount <= 0) {
                return;
            }

            groupTooltip.add(Component.translatable("jade.gas.hidden_tanks", hiddenTankCount).withStyle(ChatFormatting.GRAY));
        });
    }

    @Contract(pure = true)
    private static StorageCollection collectStorage(GasHandler gasHandler, boolean includeCapacity, boolean includePressure) {
        StorageCollection storageCollection = new StorageCollection();
        for (int tankIndex = 0; tankIndex < gasHandler.getTanks(); tankIndex++) {
            long maxAmount = includeCapacity ? getDisplayMaxAmount(gasHandler, tankIndex) : 0;
            if (includeCapacity && maxAmount <= 0) {
                continue;
            }

            storageCollection.tankCount++;
            if (!gasHandler.getGasInTank(tankIndex).isEmpty()) {
                continue;
            }

            storageCollection.emptyTankCount++;
            if (includeCapacity) {
                storageCollection.emptyMaxAmount = BoundedMath.saturatedAdd(storageCollection.emptyMaxAmount, maxAmount);
            }
            if (!includePressure) {
                continue;
            }

            storageCollection.includeEmptyPressure(getDisplayPressure(gasHandler, tankIndex));
        }

        if (storageCollection.tankCount == 0) {
            storageCollection.entries = Stream.empty();
            return storageCollection;
        }

        storageCollection.entries = IntStream.range(0, gasHandler.getTanks()).mapToObj(tankIndex -> {
            long maxAmount = includeCapacity ? getDisplayMaxAmount(gasHandler, tankIndex) : 0;
            if (includeCapacity && maxAmount <= 0) {
                return null;
            }

            GasStack gasStack = gasHandler.getGasInTank(tankIndex);
            if (gasStack.isEmpty()) {
                return null;
            }

            long pressurePa = includePressure ? getDisplayPressure(gasHandler, tankIndex) : GasStorageView.NO_PRESSURE_READING;
            return new StorageEntryData(GasStorageEntry.of(gasStack.getGasType(), gasStack.getAmount(), gasStack.getComponentsPatch()), maxAmount, pressurePa);
        }).filter(Objects::nonNull);
        return storageCollection;
    }

    private static void appendView(ITooltip tooltip, GasStorageView view, boolean showDetails, boolean includePressure, IElementHelper elementHelper) {
        Component progressText = getText(view, showDetails);
        tooltip.add(elementHelper.sprite(ICON, 16, 16));
        ProgressStyle progressStyle = elementHelper.progressStyle().overlay(view.overlay);
        tooltip.append(elementHelper.progress(view.ratio, progressText, progressStyle, BoxStyle.getNestedBox(), true));
        if (showDetails) {
            for (Component line : view.gasTooltip) {
                tooltip.add(line);
            }
        }
        if (!includePressure || view.pressurePa < GasPressure.VACUUM_PA) {
            return;
        }

        Component pressureValue = Component.literal(GasPressure.formatAtm(view.pressurePa));
        if (GasPressureLimits.isOverpressure(view.pressurePa)) {
            pressureValue = pressureValue.copy().withStyle(ChatFormatting.RED);
        }
        tooltip.add(Component.translatable("jade.gas.pressure", pressureValue));
    }

    private static Component getText(GasStorageView view, boolean showDetails) {
        if (view.overrideText != null) {
            return view.overrideText;
        }

        Component gasName = IThemeHelper.get().info(view.gasName);
        if (view.creative) {
            return Component.translatable("jade.gas.creative", gasName).withStyle(ChatFormatting.WHITE);
        }

        Component currentAmount = Component.literal(view.currentAmountText).withStyle(ChatFormatting.WHITE);
        if (showDetails && view.hasCapacity) {
            Component maxAmount = Component.literal(view.maxAmountText).withStyle(ChatFormatting.GRAY);
            return Component.translatable("jade.gas.detailed", gasName, currentAmount, maxAmount);
        }

        return Component.translatable("jade.gas", gasName, currentAmount);
    }

    private static long getDisplayMaxAmount(GasHandler gasHandler, int tankIndex) {
        if (gasHandler instanceof GasStorageHandler storage) {
            return storage.getTankMaxAmount(tankIndex);
        }

        return Math.max(0, gasHandler.getGasInTank(tankIndex).getAmount());
    }

    private static long getDisplayPressure(GasHandler gasHandler, int tankIndex) {
        if (!(gasHandler instanceof GasStorageHandler storage)) {
            return GasStorageView.NO_PRESSURE_READING;
        }

        return Math.max(GasPressure.VACUUM_PA, storage.getTankPressurePa(tankIndex));
    }

    private record StorageEntryData(GasStorageEntry entry, long maxAmount, long pressurePa) {}

    private static class StorageCollection {
        private Stream<StorageEntryData> entries;
        private int tankCount;
        private long emptyMaxAmount;
        private int emptyTankCount;
        private long emptyPressurePa = GasStorageView.NO_PRESSURE_READING;
        private boolean emptyPressureMismatch;

        private void includeEmptyPressure(long pressurePa) {
            if (pressurePa < GasPressure.VACUUM_PA || emptyPressureMismatch) {
                return;
            }

            if (emptyPressurePa < GasPressure.VACUUM_PA) {
                emptyPressurePa = pressurePa;
                return;
            }

            if (emptyPressurePa == pressurePa) {
                return;
            }

            emptyPressurePa = GasStorageView.NO_PRESSURE_READING;
            emptyPressureMismatch = true;
        }
    }
}
