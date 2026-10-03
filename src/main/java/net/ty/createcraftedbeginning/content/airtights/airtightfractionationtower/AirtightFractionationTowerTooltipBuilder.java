package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.gas.visual.GasUnitsTooltips;
import net.ty.createcraftedbeginning.platform.client.ClientContextBridge;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightFractionationTowerTooltipBuilder {
    private static final int PROGRESS_BAR_SEGMENTS = 20;

    private final AirtightFractionationTowerBlockEntity tower;

    AirtightFractionationTowerTooltipBuilder(AirtightFractionationTowerBlockEntity tower) {
        this.tower = tower;
    }

    private static void addProgressInfo(AirtightFractionationTowerCrafting crafting, List<Component> tooltip) {
        int duration = crafting.getDuration();
        if (duration <= 0) {
            return;
        }

        int progress = Mth.clamp(crafting.getProgress(), 0, duration);
        double filled = (double) progress * PROGRESS_BAR_SEGMENTS / duration;
        int completedSegments = Mth.floor(filled);
        int occupiedSegments = Mth.ceil(filled);
        MutableComponent bar = Component.empty();
        for (int segment = 1; segment <= PROGRESS_BAR_SEGMENTS; segment++) {
            ChatFormatting color;
            if (segment > occupiedSegments) {
                color = ChatFormatting.DARK_RED;
            }
            else if (segment > completedSegments) {
                color = ChatFormatting.YELLOW;
            }
            else {
                color = ChatFormatting.DARK_GREEN;
            }
            bar.append(Component.literal("|").withStyle(color));
        }
        int percentage = (int) ((long) progress * 100 / duration);
        bar.append(CCBLang.text(' ' + String.valueOf(percentage) + '%').style(ChatFormatting.GRAY).component());
        if (crafting.isPaused()) {
            String pauseKey = switch (crafting.getPauseReason()) {
                case TEMPERATURE -> "gui.airtight_fractionation_tower.progress_paused.temperature";
                case OUTPUT -> "gui.airtight_fractionation_tower.progress_paused.output";
                case STRUCTURE -> "gui.airtight_fractionation_tower.progress_paused.structure";
                case OTHER -> "gui.airtight_fractionation_tower.progress_paused";
            };
            bar.append(CCBLang.translateDirect(pauseKey).withStyle(ChatFormatting.YELLOW));
        }

        CCBLang.translate("gui.airtight_fractionation_tower.progress").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.builder().add(bar).forGoggles(tooltip, 1);
    }

    boolean addToGoggleTooltip(List<Component> tooltip) {
        BlockPos origin = tower.getOrigin();
        if (origin == null || tower.isRemoved()) {
            return false;
        }

        int layer = tower.getBlockPos().getY() - origin.getY() + 1;
        CCBLang.translate("gui.airtight_fractionation_tower").add(CCBLang.translate("gui.airtight_fractionation_tower.layer", layer).style(ChatFormatting.GRAY)).forGoggles(tooltip);
        if (addStoredInfo(tooltip)) {
            tooltip.add(CommonComponents.EMPTY);
        }

        AirtightFractionationTowerBlockEntity controller = tower.findTowerController();
        if (controller == null) {
            CCBLang.translate("gui.airtight_fractionation_tower.controller_unavailable").style(ChatFormatting.GRAY).forGoggles(tooltip);
            return true;
        }

        addProgressInfo(controller.getCrafting(), tooltip);
        AirtightFractionationTowerStructureManager structureManager = controller.getStructureManager();
        TemperatureCondition condition = TemperatureCondition.getConditionByTemperature(structureManager.getTemperature());
        String modeKey = switch (structureManager.getProcessingMode()) {
            case NONE -> "none";
            case FRACTIONATION -> "fractionation";
            case CONDENSATION -> "condensation";
        };
        CCBLang.translate("gui.airtight_fractionation_tower.mode").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate("gui.airtight_fractionation_tower.mode." + modeKey).color(condition.getColor()).forGoggles(tooltip, 1);
        CCBLang.translate("gui.airtight_fractionation_tower.temperature_state").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.translate(condition.getTranslationKey()).color(condition.getColor()).forGoggles(tooltip, 1);
        return true;
    }

    private boolean addStoredInfo(List<Component> tooltip) {
        int contentsStartIndex = tooltip.size();
        CCBLang.translate("gui.airtight_fractionation_tower.contents").style(ChatFormatting.GRAY).forGoggles(tooltip);

        int maxItemDisplay = ClientContextBridge.getMaxItemStackDisplay();
        int itemCount = addItemInfo(tooltip, maxItemDisplay);
        if (itemCount > maxItemDisplay) {
            CCBLang.translate("gui.airtight_fractionation_tower.more", itemCount - maxItemDisplay).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 2);
        }

        int storedEntryCount = itemCount + addFluidInfo(tooltip) + addGasInfo(tooltip);
        if (storedEntryCount > 0) {
            return true;
        }

        while (tooltip.size() > contentsStartIndex) {
            tooltip.removeLast();
        }
        return false;
    }

    private int addItemInfo(List<Component> tooltip, int maxItemDisplay) {
        if (!GoggleTooltip.isVisible(tooltip, Section.ITEM_STORAGE)) {
            return 0;
        }

        int itemCount = 0;
        IItemHandler items = tower.getItemCapability();
        if (items == null) {
            return 0;
        }

        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack itemStack = items.getStackInSlot(slot);
            if (itemStack.isEmpty()) {
                continue;
            }

            if (itemCount == 0) {
                CCBLang.translate("gui.airtight_fractionation_tower.items").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            if (itemCount < maxItemDisplay) {
                CCBLang.itemName(itemStack).style(ChatFormatting.GRAY).add(CCBLang.text(" x" + itemStack.getCount()).style(ChatFormatting.GREEN)).forGoggles(tooltip, 2);
            }
            itemCount++;
        }
        return itemCount;
    }

    private int addFluidInfo(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.FLUID_STORAGE)) {
            return 0;
        }

        int fluidCount = 0;
        IFluidHandler fluids = tower.getFluidCapability();
        if (fluids == null) {
            return 0;
        }

        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack fluidStack = fluids.getFluidInTank(tank);
            if (fluidStack.isEmpty()) {
                continue;
            }

            if (fluidCount == 0) {
                CCBLang.translate("gui.airtight_fractionation_tower.fluids").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            LangBuilder unit = CCBLang.translate("gui.unit.milli_buckets");
            CCBLang.fluidName(fluidStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(CCBLang.number(fluidStack.getAmount()).space().add(unit).style(ChatFormatting.BLUE)).forGoggles(tooltip, 2);
            fluidCount++;
        }
        return fluidCount;
    }

    private int addGasInfo(List<Component> tooltip) {
        if (!GoggleTooltip.isVisible(tooltip, Section.GAS_STORAGE)) {
            return 0;
        }

        int gasCount = 0;
        GasStorageHandler gases = tower.getGasCapability();
        if (gases == null) {
            return 0;
        }

        for (int tank = 0; tank < gases.getTanks(); tank++) {
            GasStack gasStack = gases.getGasInTank(tank);
            if (gasStack.isEmpty()) {
                continue;
            }

            if (gasCount == 0) {
                CCBLang.translate("gui.airtight_fractionation_tower.gases").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            }
            CCBLang.gasName(gasStack).add(CCBLang.text(" ")).style(ChatFormatting.GRAY).add(GasUnitFormat.amount(gasStack.getAmount()).style(ChatFormatting.AQUA)).forGoggles(tooltip, 2);
            GasUnitsTooltips.addPressureReading(tooltip, gases, tank, 2, tower);
            gasCount++;
        }
        return gasCount;
    }
}
