package net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower.CinderIncinerationBlowerBlockEntity.MAX_RANGE;

public class CinderIncinerationBlowerScrollValueBehaviour extends ScrollValueBehaviour {
    public CinderIncinerationBlowerScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        withFormatter(String::valueOf);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.translatable("appliances.cinder_incineration_blower.working_range_cube_edge_length").withStyle(ChatFormatting.BOLD));
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(label, MAX_RANGE, 2, rows, formatter);
    }

    @Override
    public void setValueSettings(Player player, @NotNull ValueSettings valueSetting, boolean ctrlHeld) {
        int value = valueSetting.value();
        if (!valueSetting.equals(getValueSettings())) {
            playFeedbackSound(this);
        }
        setValue(value);
    }

    @Override
    public ValueSettings getValueSettings() {
        return super.getValueSettings();
    }

    public MutableComponent formatSettings(@NotNull ValueSettings settings) {
        return CreateLang.number(settings.value()).add(Component.translatable("appliances.cinder_incineration_blower.working_range_unit")).style(ChatFormatting.BOLD).component();
    }

    @Override
    public String getClipboardKey() {
        return "ScrollValue";
    }
}
