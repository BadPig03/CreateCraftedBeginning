package net.ty.createcraftedbeginning.compat.jade.gas;

import com.google.common.math.LongMath;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
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
public class GasDataProvider {
    public static final String STORAGE_KEY = "JadeGasStorage";
    public static final String STORAGE_UID_KEY = "JadeGasStorageUid";

    private static final ResourceLocation ICON = CCBAPI.asResource("icon");

    public static @Unmodifiable List<ViewGroup<CompoundTag>> fromGasHandler(IGasHandler gasHandler, boolean creative) {
        GasCollectingResult collectedGases = fromGasHandlerStream(gasHandler);
        if (collectedGases.tankCount == 0) {
            return List.of();
        }

        List<Tuple<GasObject, Long>> displayedEntries = new ArrayList<>();
        int maxDisplayedEntries = collectedGases.emptyTankCount == 0 ? 5 : 4;
        if (collectedGases.tankCount - collectedGases.emptyTankCount <= maxDisplayedEntries) {
            displayedEntries.addAll(collectedGases.entries.toList());
        }
        else {
            collectedGases.entries.takeWhile(entry -> displayedEntries.size() < maxDisplayedEntries).forEach(candidate -> {
                for (Tuple<GasObject, Long> displayedEntry : displayedEntries) {
                    if (GasObject.isSameGasSameComponents(candidate.getA(), displayedEntry.getA())) {
                        return;
                    }
                }

                displayedEntries.add(candidate);
            });
        }

        int hiddenTankCount = collectedGases.tankCount - collectedGases.emptyTankCount - displayedEntries.size();
        if (collectedGases.emptyTankCount > 0) {
            displayedEntries.add(new Tuple<>(GasObject.empty(), collectedGases.emptyCapacity));
        }

        List<CompoundTag> serializedViews = displayedEntries.stream().map(entry -> GasView.writeDefault(entry.getA(), entry.getB(), creative)).toList();
        ViewGroup<CompoundTag> group = new ViewGroup<>(serializedViews);
        if (hiddenTankCount <= 0) {
            return List.of(group);
        }

        group.getExtraData().putInt("+", hiddenTankCount);
        return List.of(group);
    }

    public static void readData(CompoundTag data, Set<IGasHandler> gasHandlers, ResourceLocation location, boolean creative) {
        List<ViewGroup<CompoundTag>> groups = new ArrayList<>();
        for (IGasHandler gasHandler : gasHandlers) {
            groups.addAll(fromGasHandler(gasHandler, creative));
        }
        ViewGroup.saveList(data, STORAGE_KEY, groups, Function.identity());
        data.putString(STORAGE_UID_KEY, location.toString());
    }

    public static void appendData(ITooltip tooltip, CompoundTag data, boolean showDetails) {
        List<ViewGroup<CompoundTag>> groups;
        try {
            groups = ViewGroup.readList(data, STORAGE_KEY, Function.identity());
        } catch (Exception exception) {
            CCBAPI.LOGGER.error("Failed to read gas storage data", exception);
            return;
        }

        if (groups == null || groups.isEmpty()) {
            return;
        }

        List<ClientViewGroup<GasView>> clientGroups = new ArrayList<>();
        for (ViewGroup<CompoundTag> group : groups) {
            List<GasView> views = group.views.stream().map(GasView::readDefault).filter(Objects::nonNull).collect(Collectors.toList());
            if (views.isEmpty()) {
                continue;
            }

            clientGroups.add(new ClientViewGroup<>(views));
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
            for (GasView view : group.views) {
                appendView(tooltip, view, showDetails, elementHelper);
            }
        });
    }

    @Contract(pure = true)
    private static GasCollectingResult fromGasHandlerStream(IGasHandler gasHandler) {
        GasCollectingResult collectedGases = new GasCollectingResult();
        for (int tankIndex = 0; tankIndex < gasHandler.getTanks(); tankIndex++) {
            long capacity = gasHandler.getTankCapacity(tankIndex);
            if (capacity <= 0) {
                continue;
            }

            collectedGases.tankCount++;
            if (!gasHandler.getGasInTank(tankIndex).isEmpty()) {
                continue;
            }

            collectedGases.emptyTankCount++;
            collectedGases.emptyCapacity = LongMath.saturatedAdd(collectedGases.emptyCapacity, capacity);
        }

        if (collectedGases.tankCount == 0) {
            collectedGases.entries = Stream.empty();
            return collectedGases;
        }

        collectedGases.entries = IntStream.range(0, gasHandler.getTanks()).mapToObj(tankIndex -> {
            long capacity = gasHandler.getTankCapacity(tankIndex);
            if (capacity <= 0) {
                return null;
            }

            GasStack gasStack = gasHandler.getGasInTank(tankIndex);
            if (gasStack.isEmpty()) {
                return null;
            }
            return new Tuple<>(GasObject.of(gasStack.getGasType(), gasStack.getAmount(), gasStack.getComponentsPatch()), capacity);
        }).filter(Objects::nonNull);
        return collectedGases;
    }

    private static void appendView(ITooltip tooltip, GasView view, boolean showDetails, IElementHelper elementHelper) {
        Component progressText = getText(view, showDetails);
        ProgressStyle progressStyle = elementHelper.progressStyle().overlay(view.overlay);
        tooltip.add(elementHelper.sprite(ICON, 16, 16));
        tooltip.append(elementHelper.progress(view.ratio, progressText, progressStyle, BoxStyle.getNestedBox(), true));
    }

    private static Component getText(GasView view, boolean showDetails) {
        if (view.overrideText != null) {
            return view.overrideText;
        }

        Component gasName = IThemeHelper.get().info(view.gasName);
        if (view.creative) {
            return Component.translatable("jade.gas.creative", gasName).withStyle(ChatFormatting.WHITE);
        }

        Component currentAmount = Component.literal(view.current).withStyle(ChatFormatting.WHITE);
        if (showDetails) {
            Component capacity = Component.literal(view.max).withStyle(ChatFormatting.GRAY);
            return Component.translatable("jade.gas.detailed", gasName, currentAmount, capacity);
        }

        return Component.translatable("jade.gas", gasName, currentAmount);
    }

    private static class GasCollectingResult {
        public Stream<Tuple<GasObject, Long>> entries;
        public int tankCount;
        private long emptyCapacity;
        private int emptyTankCount;
    }
}
