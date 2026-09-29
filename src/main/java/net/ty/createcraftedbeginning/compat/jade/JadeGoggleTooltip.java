package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.IBoxElement;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class JadeGoggleTooltip {
    private static final ResourceLocation GOGGLES = ResourceLocation.fromNamespaceAndPath("jadeaddons.create", "goggles");
    private static final ResourceLocation REQUIRES_GOGGLES = ResourceLocation.fromNamespaceAndPath("jadeaddons.create", "goggles.requires_goggles");
    private static final ResourceLocation DETAILED = ResourceLocation.fromNamespaceAndPath("jadeaddons.create", "goggles.detailed");

    private JadeGoggleTooltip() {
    }

    static void onTooltipCollected(IBoxElement box, Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor blockAccessor)) {
            return;
        }

        ITooltip tooltip = box.getTooltip();
        if (tooltip.get(GOGGLES).isEmpty()) {
            return;
        }

        IPluginConfig config = IWailaConfig.get().getPlugin();
        if (!config.get(GOGGLES) || config.get(DETAILED) && !accessor.showDetails()) {
            return;
        }

        Level level = accessor.getLevel();
        BlockEntity source = level.getBlockEntity(GoggleOverlayRenderer.proxiedOverlayPosition(level, blockAccessor.getPosition()));
        if (source == null || source.isRemoved() || !CCBAPI.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(source.getBlockState().getBlock()).getNamespace())) {
            return;
        }

        Set<Section> hiddenSections = EnumSet.noneOf(Section.class);
        if (!tooltip.get(JadePlugin.GAS_STORAGE_BLOCK_TOOLTIP).isEmpty()) {
            hiddenSections.add(Section.GAS_STORAGE);
        }
        if (!tooltip.get(GasStorageTooltipProvider.OVERPRESSURE_TOOLTIP).isEmpty()) {
            hiddenSections.add(Section.OVERPRESSURE);
        }
        if (!tooltip.get(JadePlugin.GAS_PIPE_TELEMETRY_TOOLTIP).isEmpty()) {
            hiddenSections.add(Section.PIPE_TELEMETRY);
        }
        if (!tooltip.get(CCBBlockEntities.BREEZE_CHAMBER.getId()).isEmpty() || !tooltip.get(CCBBlockEntities.BREEZE_COOLER.getId()).isEmpty()) {
            hiddenSections.add(Section.BREEZE_TIME);
        }
        if (!tooltip.get(JadeIds.UNIVERSAL_FLUID_STORAGE).isEmpty()) {
            hiddenSections.add(Section.FLUID_STORAGE);
        }
        if (!tooltip.get(JadeIds.UNIVERSAL_ITEM_STORAGE).isEmpty()) {
            BlockEntity observed = blockAccessor.getBlockEntity();
            if (source instanceof AirtightForgingPressBlockEntity) {
                if (observed instanceof AirtightForgingPressBlockEntity) {
                    hiddenSections.add(Section.PRESS_HEAD);
                }
                else if (observed instanceof AirtightForgingPressStructuralShaftBlockEntity) {
                    hiddenSections.add(Section.PROCESSING_MATERIAL);
                }
                else if (observed instanceof AirtightForgingPressStructuralBlockEntity) {
                    hiddenSections.add(Section.ITEM_STORAGE);
                }
            }
            else {
                hiddenSections.add(Section.ITEM_STORAGE);
            }
        }
        if (hiddenSections.isEmpty()) {
            return;
        }

        List<Component> lines = new GoggleTooltip(hiddenSections);
        if (source instanceof IHaveGoggleInformation goggles && (!config.get(REQUIRES_GOGGLES) || GogglesItem.isWearingGoggles(accessor.getPlayer()))) {
            goggles.addToGoggleTooltip(lines, accessor.showDetails());
        }
        if (source instanceof IHaveHoveringInformation hovering) {
            List<Component> warnings = new ArrayList<>();
            if (hovering.addToTooltip(warnings, accessor.showDetails())) {
                if (!lines.isEmpty()) {
                    lines.add(Component.empty());
                }
                lines.addAll(warnings);
            }
        }

        IElementHelper elements = IElementHelper.get();
        List<List<IElement>> replacement = new ArrayList<>();
        boolean separator = false;
        for (Component line : lines) {
            if (line.getString().isBlank()) {
                separator = !replacement.isEmpty();
                continue;
            }

            if (separator) {
                replacement.add(List.of(elements.spacer(3, 3).tag(GOGGLES)));
                separator = false;
            }
            Component displayedLine = line;
            if (line.getContents() instanceof LiteralContents(String text) && text.startsWith("    ")) {
                MutableComponent unindented = Component.literal(text.substring(4)).withStyle(line.getStyle());
                line.getSiblings().forEach(unindented::append);
                displayedLine = unindented;
            }
            replacement.add(List.of(elements.text(displayedLine).tag(GOGGLES)));
        }
        if (replacement.isEmpty()) {
            tooltip.remove(GOGGLES);
            return;
        }

        tooltip.replace(GOGGLES, ignored -> replacement);
    }
}
