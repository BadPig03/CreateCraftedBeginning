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
        GasCollectingResult result = fromGasHandlerStream(gasHandler);
        if (result.tanks == 0) {
            return List.of();
        }

        List<Tuple<GasObject, Long>> entries = new ArrayList<>();
        int maxEntries = result.emptyTanks == 0 ? 5 : 4;
        if (result.tanks - result.emptyTanks <= maxEntries) {
            entries.addAll(result.stream.toList());
        }
        else {
            result.stream.takeWhile(entry -> entries.size() < maxEntries).forEach(candidate -> {
                for (Tuple<GasObject, Long> entry : entries) {
                    if (GasObject.isSameGasSameComponents(candidate.getA(), entry.getA())) {
                        return;
                    }
                }

                entries.add(candidate);
            });
        }

        int remaining = result.tanks - result.emptyTanks - entries.size();
        if (result.emptyTanks > 0) {
            entries.add(new Tuple<>(GasObject.empty(), result.emptyCapacity));
        }

        List<CompoundTag> views = entries.stream().map(entry -> GasView.writeDefault(entry.getA(), entry.getB(), creative)).toList();
        ViewGroup<CompoundTag> group = new ViewGroup<>(views);
        if (remaining <= 0) {
            return List.of(group);
        }

        group.getExtraData().putInt("+", remaining);
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

        IElementHelper helper = IElementHelper.get();
        boolean renderGroup = clientGroups.size() > 1 || clientGroups.getFirst().shouldRenderGroup();
        ClientViewGroup.tooltip(tooltip, clientGroups, renderGroup, (groupTooltip, group) -> {
            if (renderGroup && group.shouldRenderGroup()) {
                group.renderHeader(groupTooltip);
            }
            for (GasView view : group.views) {
                appendView(tooltip, view, showDetails, helper);
            }
        });
    }

    @Contract(pure = true)
    private static GasCollectingResult fromGasHandlerStream(IGasHandler gasHandler) {
        GasCollectingResult result = new GasCollectingResult();
        for (int i = 0; i < gasHandler.getTanks(); i++) {
            long capacity = gasHandler.getTankCapacity(i);
            if (capacity <= 0) {
                continue;
            }

            result.tanks++;
            if (!gasHandler.getGasInTank(i).isEmpty()) {
                continue;
            }

            result.emptyTanks++;
            result.emptyCapacity = LongMath.saturatedAdd(result.emptyCapacity, capacity);
        }

        if (result.tanks == 0) {
            result.stream = Stream.empty();
            return result;
        }

        result.stream = IntStream.range(0, gasHandler.getTanks()).mapToObj(i -> {
            long capacity = gasHandler.getTankCapacity(i);
            if (capacity <= 0) {
                return null;
            }

            GasStack gas = gasHandler.getGasInTank(i);
            if (gas.isEmpty()) {
                return null;
            }
            return new Tuple<>(GasObject.of(gas.getGasType(), gas.getAmount(), gas.getComponentsPatch()), capacity);
        }).filter(Objects::nonNull);
        return result;
    }

    private static void appendView(ITooltip tooltip, GasView view, boolean showDetails, IElementHelper helper) {
        Component text = getText(view, showDetails);
        ProgressStyle style = helper.progressStyle().overlay(view.overlay);
        tooltip.add(helper.sprite(ICON, 16, 16));
        tooltip.append(helper.progress(view.ratio, text, style, BoxStyle.getNestedBox(), true));
    }

    private static Component getText(GasView view, boolean showDetails) {
        if (view.overrideText != null) {
            return view.overrideText;
        }

        Component name = IThemeHelper.get().info(view.gasName);
        if (view.creative) {
            return Component.translatable("jade.gas.creative", name).withStyle(ChatFormatting.WHITE);
        }

        Component current = Component.literal(view.current).withStyle(ChatFormatting.WHITE);
        if (showDetails) {
            Component max = Component.literal(view.max).withStyle(ChatFormatting.GRAY);
            return Component.translatable("jade.gas.detailed", name, current, max);
        }

        return Component.translatable("jade.gas", name, current);
    }

    private static class GasCollectingResult {
        public Stream<Tuple<GasObject, Long>> stream;
        public int tanks;
        private long emptyCapacity;
        private int emptyTanks;
    }
}
