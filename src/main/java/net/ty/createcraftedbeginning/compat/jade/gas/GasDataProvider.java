package net.ty.createcraftedbeginning.compat.jade.gas;

import com.google.common.math.LongMath;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.IGasHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import snownee.jade.api.ITooltip;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class GasDataProvider {
    private static final ResourceLocation ICON = CreateCraftedBeginning.asResource("icon");

    @Contract(pure = true)
    public static @NotNull GasCollectingResult fromGasHandlerStream(@NotNull IGasHandler gasHandler) {
        GasCollectingResult result = new GasCollectingResult();
        for (int i = 0; i < gasHandler.getTanks(); i++) {
            long capacity = gasHandler.getTankCapacity(i);
            if (capacity > 0) {
                result.tanks++;
                if (gasHandler.getGasInTank(i).isEmpty()) {
                    result.emptyTanks++;
                    result.emptyCapacity = LongMath.saturatedAdd(result.emptyCapacity, capacity);
                }
            }
        }

        if (result.tanks == 0) {
            result.stream = Stream.empty();
        }
        else {
            result.stream = IntStream.range(0, gasHandler.getTanks()).mapToObj(i -> {
                long capacity = gasHandler.getTankCapacity(i);
                if (capacity <= 0) {
                    return null;
                }
                else {
                    GasStack gasStack = gasHandler.getGasInTank(i);
                    return gasStack.isEmpty() ? null : new Tuple<>(GasObject.of(gasStack.getGas(), gasStack.getAmount(), gasStack.getComponentsPatch()), capacity);
                }
            }).filter(Objects::nonNull);
        }
        return result;
    }

    public static @NotNull @Unmodifiable List<ViewGroup<CompoundTag>> fromGasHandler(IGasHandler gasHandler, boolean creative) {
        GasCollectingResult result = fromGasHandlerStream(gasHandler);
        if (result.tanks == 0) {
            return List.of();
        }

        List<Tuple<GasObject, Long>> list = new ArrayList<>();
        int maxTanks = result.emptyTanks == 0 ? 5 : 4;
        if (result.tanks - result.emptyTanks <= maxTanks) {
            list.addAll(result.stream.toList());
        }
        else {
            result.stream.takeWhile(tag -> list.size() <= maxTanks).forEach(tuple1 -> {
                for (Tuple<GasObject, Long> tuple2 : list) {
                    if (GasObject.isSameGasSameComponents(tuple1.getA(), tuple2.getA())) {
                        return;
                    }
                }
                list.add(tuple1);
            });
        }
        int remaining = result.tanks - result.emptyTanks - list.size();
        if (result.emptyTanks > 0) {
            list.add(new Tuple<>(GasObject.empty(), result.emptyCapacity));
        }
        ViewGroup<CompoundTag> group = new ViewGroup<>(list.stream().map(tuple -> GasView.writeDefault(tuple.getA(), tuple.getB(), creative)).toList());
        if (remaining > 0) {
            group.getExtraData().putInt("+", remaining);
        }
        return List.of(group);
    }

    public static void readData(CompoundTag data, @NotNull Set<IGasHandler> gasHandlers, @NotNull ResourceLocation location, boolean creative) {
        List<ViewGroup<CompoundTag>> groups = new ArrayList<>();
        for (IGasHandler gasHandler : gasHandlers) {
            groups.addAll(fromGasHandler(gasHandler, creative));
        }
        ViewGroup.saveList(data, GasConstants.STORAGE_KEY, groups, Function.identity());
        data.putString(GasConstants.STORAGE_UID_KEY, location.toString());
    }

    public static void appendData(ITooltip tooltip, CompoundTag data, boolean showDetails) {
        List<ViewGroup<CompoundTag>> groups;
        try {
            groups = ViewGroup.readList(data, GasConstants.STORAGE_KEY, Function.identity());
        } catch (Exception e) {
            CreateCraftedBeginning.LOGGER.error("Failed to read gas storage data", e);
            return;
        }

        if (groups == null || groups.isEmpty()) {
            return;
        }

        List<ClientViewGroup<GasView>> clientGroups = new ArrayList<>();
        for (ViewGroup<CompoundTag> group : groups) {
            List<GasView> views = group.views.stream().map(GasView::readDefault).filter(view -> view != null).collect(Collectors.toList());
            if (!views.isEmpty()) {
                clientGroups.add(new ClientViewGroup<>(views));
            }
        }
        if (clientGroups.isEmpty()) {
            return;
        }

        IElementHelper helper = IElementHelper.get();
        boolean renderGroup = clientGroups.size() > 1 || clientGroups.getFirst().shouldRenderGroup();
        ClientViewGroup.tooltip(tooltip, clientGroups, renderGroup, (iTooltip, currentGroup) -> {
            if (renderGroup && currentGroup.shouldRenderGroup()) {
                currentGroup.renderHeader(iTooltip);
            }
            for (GasView view : currentGroup.views) {
                Component text;
                if (view.overrideText != null) {
                    text = view.overrideText;
                }
                else {
                    Component name = IThemeHelper.get().info(view.gasName);
                    if (view.creative) {
                        text = Component.translatable("jade.gas.creative", name).withStyle(ChatFormatting.WHITE);
                    }
                    else {
                        text = showDetails ? Component.translatable("jade.gas.detailed", name, Component.literal(view.current).withStyle(ChatFormatting.WHITE), Component.literal(view.max).withStyle(ChatFormatting.GRAY)) : Component.translatable("jade.gas", name, Component.literal(view.current).withStyle(ChatFormatting.WHITE));
                    }
                }
                ProgressStyle progressStyle = helper.progressStyle().overlay(view.overlay);
                tooltip.add(helper.sprite(ICON, 16, 16));
                tooltip.append(helper.progress(view.ratio, text, progressStyle, BoxStyle.getNestedBox(), true));
            }
        });
    }

    public static class GasCollectingResult {
        public Stream<Tuple<GasObject, Long>> stream;
        public long emptyCapacity;
        public int tanks;
        public int emptyTanks;
    }
}
