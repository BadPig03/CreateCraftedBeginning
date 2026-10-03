package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.content.opticalpower.laser.LaserBehaviour;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterRangeBehaviour extends ScrollValueBehaviour {
    private static final String COMPOUND_KEY_LASER_RANGE = "LaserRange";
    private static final int RANGE_BOARD_INTERVAL = 8;

    LaserEmitterRangeBehaviour(LaserEmitterBlockEntity emitter) {
        super(CCBLang.translateDirect("gui.laser_emitter.range"), emitter, new LaserEmitterRangeValueBox());
        between(LaserBehaviour.MIN_RANGE, LaserBehaviour.MAX_RANGE);
        value = LaserBehaviour.MAX_RANGE;
    }

    @Override
    public void write(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        compoundTag.putInt(COMPOUND_KEY_LASER_RANGE, value);
    }

    @Override
    public void read(CompoundTag compoundTag, Provider registries, boolean clientPacket) {
        if (!compoundTag.contains(COMPOUND_KEY_LASER_RANGE)) {
            value = LaserBehaviour.MAX_RANGE;
            return;
        }

        value = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_LASER_RANGE), LaserBehaviour.MIN_RANGE, LaserBehaviour.MAX_RANGE);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(label, LaserBehaviour.MAX_RANGE - LaserBehaviour.MIN_RANGE, RANGE_BOARD_INTERVAL, List.of(CCBLang.translateDirect("gui.laser_emitter.range_unit")), new ValueSettingsFormatter(settings -> CCBLang.number(settings.value() + LaserBehaviour.MIN_RANGE).component()));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        int range = Mth.clamp(settings.value(), 0, LaserBehaviour.MAX_RANGE - LaserBehaviour.MIN_RANGE) + LaserBehaviour.MIN_RANGE;
        if (range == value) {
            return;
        }

        setValue(range);
        playFeedbackSound(this);
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value - LaserBehaviour.MIN_RANGE);
    }
}
