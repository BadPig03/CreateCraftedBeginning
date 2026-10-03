package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.MutableComponent;
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
final class AirtightRegulatorPumpPressureBehaviour extends ScrollValueBehaviour {
    private static final int STEPS_PER_ATMOSPHERE = 2;
    private static final int DEFAULT_OUTLET_SET_PRESSURE_STEPS = 8;
    private static final int MAX_OUTLET_SET_PRESSURE_STEPS = 32;

    private final AirtightRegulatorPumpBlockEntity owner;

    AirtightRegulatorPumpPressureBehaviour(AirtightRegulatorPumpBlockEntity owner) {
        super(CCBLang.translateDirect("gui.airtight_regulator_pump.outlet_set_pressure_scroll"), owner, new AirtightRegulatorPumpPressureValueBox());
        this.owner = owner;
        between(0, MAX_OUTLET_SET_PRESSURE_STEPS);
        withCallback(ignored -> owner.applyOutletSetPressureFromBehaviour(pressurePaForStep(value)));
        withFormatter(AirtightRegulatorPumpPressureBehaviour::formatPressureStep);
        value = pressureStepForPa(owner.getOutletSetPressurePa());
    }

    static long defaultOutletSetPressurePa() {
        return normalizePressurePa(pressurePaForUnclampedStep(DEFAULT_OUTLET_SET_PRESSURE_STEPS));
    }

    static long maxOutletSetPressurePa() {
        return pressurePaForUnclampedStep(MAX_OUTLET_SET_PRESSURE_STEPS);
    }

    static long normalizePressurePa(long pressurePa) {
        return pressurePaForStep(pressureStepForPa(pressurePa));
    }

    private static MutableComponent formatPressureSetting(ValueSettings settings) {
        MutableComponent component = CCBLang.text(formatPressureStep(settings.value())).component();
        if (pressurePaForStep(settings.value()) > GasPressureLimits.SAFE_PRESSURE_PA) {
            component.withStyle(ChatFormatting.RED);
        }
        return component;
    }

    private static String formatPressureStep(int pressureStep) {
        double pressureAtm = (double) Mth.clamp(pressureStep, 0, MAX_OUTLET_SET_PRESSURE_STEPS) / STEPS_PER_ATMOSPHERE;
        return String.format(Locale.ROOT, "%.1f", pressureAtm);
    }

    private static int pressureStepForPa(long pressurePa) {
        double pressureAtm = (double) Math.max(GasPressure.VACUUM_PA, pressurePa) / GasPressure.REFERENCE_PRESSURE_PA;
        return Mth.clamp((int) Math.round(pressureAtm * STEPS_PER_ATMOSPHERE), 0, MAX_OUTLET_SET_PRESSURE_STEPS);
    }

    private static long pressurePaForStep(int pressureStep) {
        return pressurePaForUnclampedStep(Mth.clamp(pressureStep, 0, MAX_OUTLET_SET_PRESSURE_STEPS));
    }

    private static long pressurePaForUnclampedStep(int pressureStep) {
        return Math.round((double) Math.max(0, pressureStep) * GasPressure.REFERENCE_PRESSURE_PA / STEPS_PER_ATMOSPHERE);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, max, 2, List.of(CCBLang.text("atm").component()), new ValueSettingsFormatter(AirtightRegulatorPumpPressureBehaviour::formatPressureSetting));
    }

    @Override
    public void tick() {
        super.tick();
        syncFromOwner();
    }

    void syncFromOwner() {
        value = pressureStepForPa(owner.getOutletSetPressurePa());
    }
}
