package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchTargetPressureBehaviour extends ScrollValueBehaviour {
    static final BehaviourType<AirtightHatchTargetPressureBehaviour> TYPE = new BehaviourType<>();
    static final String NBT_KEY = "HatchTargetPressure";
    static final int STEPS_PER_ATMOSPHERE = 2;
    static final int MAX_PRESSURE_ATM = GasPressureLimits.HARD_PRESSURE_ATM;
    static final int MAX_PRESSURE_STEPS = MAX_PRESSURE_ATM * STEPS_PER_ATMOSPHERE;
    private static final long DEFAULT_TARGET_PRESSURE_PA = GasPressure.pascals(5);

    private final AirtightHatchBlockEntity owner;

    AirtightHatchTargetPressureBehaviour(AirtightHatchBlockEntity owner) {
        super(CCBLang.translateDirect("gui.airtight_hatch.target_pressure"), owner, new AirtightHatchTargetPressureValueBox());
        this.owner = owner;
        between(0, MAX_PRESSURE_STEPS);
        withFormatter(AirtightHatchTargetPressureBehaviour::formatPressureStep);
        onlyActiveWhen(owner::isTargetPressureControlActive);
        value = pressureStepForPa(DEFAULT_TARGET_PRESSURE_PA);
    }

    @Override
    public String getClipboardKey() {
        return "HatchTargetPressure";
    }

    @Override
    public int netId() {
        return 1;
    }

    @Override
    public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
        nbt.putInt(NBT_KEY, value);
    }

    @Override
    public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
        int maxSteps = maxPressureStepsForCanister();
        between(0, maxSteps);
        if (nbt.contains(NBT_KEY)) {
            value = Mth.clamp(nbt.getInt(NBT_KEY), 0, maxSteps);
            return;
        }

        long maxPressurePa = owner.getCanisterMaxPressurePa();
        long fallbackPressurePa = maxPressurePa > GasPressure.VACUUM_PA ? maxPressurePa / 2 : DEFAULT_TARGET_PRESSURE_PA;
        value = Mth.clamp(pressureStepForPa(fallbackPressurePa), 0, maxSteps);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        syncRangeToCanister();
        return new ValueSettingsBoard(label, max, 2, List.of(CCBLang.text("atm").component()), new ValueSettingsFormatter(AirtightHatchTargetPressureBehaviour::formatPressureSetting));
    }

    @Override
    public void tick() {
        super.tick();
        syncRangeToCanister();
    }

    long getTargetPressurePa() {
        return pressurePaForStep(value);
    }

    void setTargetPressurePa(long pressurePa) {
        syncRangeToCanister();
        setValue(pressureStepForPa(pressurePa));
    }

    private static MutableComponent formatPressureSetting(ValueSettings settings) {
        MutableComponent component = CCBLang.text(formatPressureStep(settings.value())).component();
        if (pressurePaForStep(settings.value()) > GasPressureLimits.SAFE_PRESSURE_PA) {
            component.withStyle(ChatFormatting.RED);
        }
        return component;
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

    private void syncRangeToCanister() {
        int maxSteps = maxPressureStepsForCanister();
        between(0, maxSteps);
        if (value <= maxSteps) {
            return;
        }

        value = maxSteps;
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        owner.setChanged();
        owner.sendData();
    }

    private int maxPressureStepsForCanister() {
        if (owner.isEmpty()) {
            return MAX_PRESSURE_STEPS;
        }

        return Mth.clamp(pressureStepForPa(owner.getCanisterMaxPressurePa()), 0, MAX_PRESSURE_STEPS);
    }
}
