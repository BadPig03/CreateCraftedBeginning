package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLensFocusBehaviour extends ScrollValueBehaviour {
    static final int MIN_FOCAL_LENGTH = 2;
    static final int MAX_FOCAL_LENGTH = 16;
    static final int DEFAULT_FOCAL_LENGTH = 8;
    private static final int FOCUS_BOARD_INTERVAL = 2;

    AmethystLensFocusBehaviour(AmethystLensBlockEntity lens) {
        super(CCBLang.translateDirect("gui.amethyst_lens.focal_length"), lens, new AmethystLensFocusValueBox());
        between(MIN_FOCAL_LENGTH, MAX_FOCAL_LENGTH);
        value = DEFAULT_FOCAL_LENGTH;
    }

    @Override
    public void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        AmethystLensSerialization.writeFocalLength(tag, value);
    }

    @Override
    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        value = AmethystLensSerialization.readFocalLength(tag);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hit) {
        return new ValueSettingsBoard(label, MAX_FOCAL_LENGTH - MIN_FOCAL_LENGTH, FOCUS_BOARD_INTERVAL, List.of(CCBLang.translateDirect("gui.amethyst_lens.blocks")), new ValueSettingsFormatter(settings -> CCBLang.number(settings.value() + MIN_FOCAL_LENGTH).component()));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        int focalLength = Mth.clamp(settings.value(), 0, MAX_FOCAL_LENGTH - MIN_FOCAL_LENGTH) + MIN_FOCAL_LENGTH;
        if (value == focalLength) {
            return;
        }

        setValue(focalLength);
        playFeedbackSound(this);
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value - MIN_FOCAL_LENGTH);
    }
}
