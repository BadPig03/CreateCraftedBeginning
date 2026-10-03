package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class CreativeAirtightTankPressureBehaviour extends ScrollValueBehaviour {
    private static final int STEPS_PER_ATMOSPHERE = 2;
    private static final int MAX_PRESSURE_ATM = GasPressureLimits.SAFE_PRESSURE_ATM;
    private static final int MAX_PRESSURE_STEPS = MAX_PRESSURE_ATM * STEPS_PER_ATMOSPHERE;
    static final long MAX_PRESSURE_PA = GasPressure.pascals(MAX_PRESSURE_ATM);

    private final CreativeAirtightTankBlockEntity owner;

    CreativeAirtightTankPressureBehaviour(CreativeAirtightTankBlockEntity tank) {
        super(CCBLang.translateDirect("gui.creative_airtight_tank.pressure"), tank, new CreativeAirtightTankPressureValueBox());
        owner = tank;
        between(0, MAX_PRESSURE_STEPS);
        withCallback(pressureStep -> applyPressureSetting(tank, pressureStep));
        withFormatter(CreativeAirtightTankPressureBehaviour::formatPressureStep);
        requiresWrench();
        value = MAX_PRESSURE_STEPS;
    }

    static long normalizePressurePa(long pressurePa) {
        return pressurePaForStep(pressureStepForPa(pressurePa));
    }

    private static void applyPressureSetting(CreativeAirtightTankBlockEntity tank, int pressureStep) {
        CreativeAirtightTankBlockEntity controller = tank.getControllerBE();
        if (controller == null) {
            controller = tank;
        }
        controller.setLocalFixedPressurePa(pressurePaForStep(pressureStep));
    }

    private static String formatPressureStep(int pressureStep) {
        return String.format(Locale.ROOT, "%.1f", (double) Mth.clamp(pressureStep, 0, MAX_PRESSURE_STEPS) / STEPS_PER_ATMOSPHERE);
    }

    private static int pressureStepForPa(long pressurePa) {
        double pressureAtm = (double) Math.max(GasPressure.VACUUM_PA, pressurePa) / GasPressure.REFERENCE_PRESSURE_PA;
        return Mth.clamp((int) Math.round(pressureAtm * STEPS_PER_ATMOSPHERE), 0, MAX_PRESSURE_STEPS);
    }

    private static long pressurePaForStep(int pressureStep) {
        return Math.round((double) Mth.clamp(pressureStep, 0, MAX_PRESSURE_STEPS) * GasPressure.REFERENCE_PRESSURE_PA / STEPS_PER_ATMOSPHERE);
    }

    @Override
    public void tick() {
        super.tick();
        syncFromController();
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, max, 2, List.of(CCBLang.text("atm").component()), new ValueSettingsFormatter(settings -> CCBLang.text(formatPressureStep(settings.value())).component()));
    }

    void syncFromController() {
        long pressurePa = normalizePressurePa(owner.getFixedPressurePa());
        owner.mirrorLocalFixedPressurePa(pressurePa);
        value = pressureStepForPa(pressurePa);
    }
}
