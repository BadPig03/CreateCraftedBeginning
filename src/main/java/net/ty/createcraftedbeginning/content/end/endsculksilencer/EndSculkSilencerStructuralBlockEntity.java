package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerValueBox;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EndSculkSilencerStructuralBlockEntity extends EndMechanicalStructuralBlockEntity<EndSculkSilencerBlockEntity> {
    private ScrollOptionBehaviour<SilencerWorkingRange> silencerWorkingRange;

    public EndSculkSilencerStructuralBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        short range = getWorkingRange();
        return Mth.abs(getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue() * range * Mth.sqrt(range);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        silencerWorkingRange = new ScrollOptionBehaviour<>(SilencerWorkingRange.class, CCBLang.translateDirect("gui.end_sculk_silencer.working_range"), this, new EndIncinerationBlowerValueBox());
        behaviours.add(silencerWorkingRange);
    }

    public short getWorkingRange() {
        return silencerWorkingRange.get().getWorkingRange();
    }

    public enum SilencerWorkingRange implements INamedIconOptions {
        ONE_BY_ONE(CCBIcons.I_1X1, 1),
        THREE_BY_THREE(CCBIcons.I_3X3, 2),
        FIVE_BY_FIVE(CCBIcons.I_5X5, 3);

        private final String translationKey;
        private final CCBIcons icon;
        private final short workingRange;

        SilencerWorkingRange(CCBIcons icon, int workingRange) {
            this.icon = icon;
            this.workingRange = (short) workingRange;
            translationKey = "createcraftedbeginning.gui.end_sculk_silencer.working_range." + Lang.asId(name());
        }

        @Override
        public CCBIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

        public short getWorkingRange() {
            return workingRange;
        }
    }
}
