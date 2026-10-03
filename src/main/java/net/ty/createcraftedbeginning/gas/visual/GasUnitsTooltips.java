package net.ty.createcraftedbeginning.gas.visual;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility;
import net.ty.createcraftedbeginning.content.airtights.AirtightTelemetryVisibility.StorageReadout;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip;
import net.ty.createcraftedbeginning.platform.client.GoggleTooltip.Section;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasUnitsTooltips {
    private GasUnitsTooltips() {
    }

    public static void addContainer(List<Component> tooltip, GasStorageHandler handler) {
        addContainer(tooltip, handler, false, null);
    }

    public static void addContainer(List<Component> tooltip, GasStorageHandler handler, boolean infinite) {
        addContainer(tooltip, handler, infinite, null);
    }

    public static void addContainer(List<Component> tooltip, GasStorageHandler handler, boolean infinite, @Nullable BlockEntity observed) {
        if (!GoggleTooltip.isVisible(tooltip, Section.GAS_STORAGE)) {
            return;
        }

        StorageReadout readout = AirtightTelemetryVisibility.storageReadout(observed);
        CCBLang.translate("gui.gas_container").forGoggles(tooltip);
        addTankState(tooltip, handler, 0, 1, infinite, readout.capacity(), readout.pressure());
    }

    @SuppressWarnings("unused")
    public static void addTankState(List<Component> tooltip, GasStorageHandler handler, int tank, int indent) {
        addTankState(tooltip, handler, tank, indent, false, false, false);
    }

    @SuppressWarnings("unused")
    public static void addTankState(List<Component> tooltip, GasStorageHandler handler, int tank, int indent, boolean infinite) {
        addTankState(tooltip, handler, tank, indent, infinite, false, false);
    }

    public static void addPressureReading(List<Component> tooltip, GasStorageHandler handler, int tank, int indent, @Nullable BlockEntity observed) {
        if (!AirtightTelemetryVisibility.canReadStoragePressure(observed) || tank < 0 || tank >= handler.getTanks()) {
            return;
        }

        long pressurePa = handler.getTankPressurePa(tank);
        CCBLang.translate("gui.gas_container.pressure").style(ChatFormatting.GRAY).add(CCBLang.text(GasPressure.formatAtm(pressurePa)).style(pressureValueColor(pressurePa))).forGoggles(tooltip, indent);
    }

    private static void addTankState(List<Component> tooltip, GasStorageHandler handler, int tank, int indent, boolean infinite, boolean includeCapacity, boolean includePressure) {
        if (tank < 0 || tank >= handler.getTanks()) {
            return;
        }

        GasStack gasStack = handler.getGasInTank(tank);
        if (gasStack.isEmpty()) {
            CCBLang.translate("gui.gas_container.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, indent);
            if (includeCapacity || includePressure) {
                tooltip.add(CommonComponents.EMPTY);
            }
        }
        else {
            CCBLang.gasName(gasStack).style(ChatFormatting.GRAY).forGoggles(tooltip, indent);
            for (Component line : gasStack.getGasType().getTooltip(gasStack)) {
                CCBLang.builder().add(line).forGoggles(tooltip, indent);
            }
            tooltip.add(CommonComponents.EMPTY);
            CCBLang.translate("gui.gas_container.amount").style(ChatFormatting.GRAY).forGoggles(tooltip, indent);
            if (infinite) {
                CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, indent + 1);
            }
            else {
                CCBLang.text(GasUnitFormat.format(gasStack.getAmount())).style(ChatFormatting.GOLD).forGoggles(tooltip, indent + 1);
            }
        }
        if (includeCapacity) {
            CCBLang.translate("gui.gas_container.capacity").style(ChatFormatting.GRAY).forGoggles(tooltip, indent);
            if (infinite) {
                CCBLang.translate("gui.gas_container.infinity").style(ChatFormatting.GOLD).forGoggles(tooltip, indent + 1);
            }
            else {
                CCBLang.text(GasUnitFormat.format(handler.getTankMaxAmount(tank))).style(ChatFormatting.GOLD).forGoggles(tooltip, indent + 1);
            }
        }
        if (!includePressure) {
            return;
        }

        long pressurePa = handler.getTankPressurePa(tank);
        CCBLang.translate("gui.gas_container.pressure").style(ChatFormatting.GRAY).forGoggles(tooltip, indent);
        CCBLang.text(GasPressure.formatAtm(pressurePa)).style(pressureValueColor(pressurePa)).forGoggles(tooltip, indent + 1);
    }

    private static ChatFormatting pressureValueColor(long pressurePa) {
        if (GasPressureLimits.isOverpressure(pressurePa)) {
            return ChatFormatting.RED;
        }

        return ChatFormatting.GOLD;
    }
}
