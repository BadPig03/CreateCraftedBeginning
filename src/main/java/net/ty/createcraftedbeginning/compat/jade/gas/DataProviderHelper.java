package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.ITooltip;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.ProgressStyle;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.ViewGroup;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataProviderHelper {
    private static final ResourceLocation ICON = CreateCraftedBeginning.asResource("icon");

    public static void getDataFromIGasHandler(CompoundTag data, @NotNull IGasHandler gasHandler, ResourceLocation location, boolean creative) {
        List<ViewGroup<CompoundTag>> groups = new ArrayList<>();
        List<CompoundTag> groupTags = new ArrayList<>();
        for (int i = 0; i < gasHandler.getTanks(); i++) {
            GasStack gasStack = gasHandler.getGasInTank(i);
            long capacity = gasHandler.getTankCapacity(i);
            if (gasStack.isEmpty() && capacity == 0) {
                continue;
            }

            CompoundTag compoundTag = new CompoundTag();
            compoundTag.put(GasConstants.STORAGE_GAS_KEY, GasObject.CODEC.encodeStart(NbtOps.INSTANCE, new GasObject(gasStack.getGasHolder().value(), gasStack.getAmount())).result().orElse(new CompoundTag()));
            compoundTag.putLong(GasConstants.STORAGE_CAPACITY_KEY, capacity);
            compoundTag.putBoolean(GasConstants.STORAGE_CREATIVE_KEY, creative);
            groupTags.add(compoundTag);
        }
        groups.add(new ViewGroup<>(groupTags));
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
            List<GasView> views = group.views.stream().map(DataProviderHelper::readForGas).filter(view -> view != null).collect(Collectors.toList());
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

    @Nullable
    public static GasView readForGas(@NotNull CompoundTag compoundTag) {
        long capacity = compoundTag.getLong(GasConstants.STORAGE_CAPACITY_KEY);
        if (capacity <= 0) {
            return null;
        }

        GasObject gasObject = GasObject.CODEC.parse(NbtOps.INSTANCE, compoundTag.get(GasConstants.STORAGE_GAS_KEY)).result().orElse(null);
        if (gasObject == null) {
            return null;
        }

        GasView gasView = new GasView(new GasStackElement(gasObject));
        gasView.gasName = Component.translatable(gasObject.type().getTranslationKey());
        gasView.current = getDecimalFormat(gasObject.amount());
        gasView.max = getDecimalFormat(capacity);
        gasView.ratio = (float) gasObject.amount() / capacity;
        gasView.creative = compoundTag.getBoolean(GasConstants.STORAGE_CREATIVE_KEY);
        if (gasObject.isEmpty()) {
            gasView.overrideText = Component.translatable("jade.gas.empty", gasView.creative ? Component.empty() : Component.literal(gasView.max).withStyle(ChatFormatting.GRAY));
        }
        return gasView;
    }

    private static @NotNull String getDecimalFormat(long amount) {
        return new DecimalFormat("#.##").format(amount / 1000.0f) + 'B';
    }
}
