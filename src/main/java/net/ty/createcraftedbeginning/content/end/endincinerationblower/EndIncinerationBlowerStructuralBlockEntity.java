package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EndIncinerationBlowerStructuralBlockEntity extends KineticBlockEntity {
    private ScrollOptionBehaviour<BlowerWorkingMode> blowerWorkingMode;
    private EndIncinerationBlowerBlockEntity blower;

    public EndIncinerationBlowerStructuralBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        blowerWorkingMode = new ScrollOptionBehaviour<>(BlowerWorkingMode.class, CCBLang.translateDirect("gui.end_incineration_blower.working_mode"), this, new EndIncinerationBlowerValueBox());
        behaviours.add(blowerWorkingMode);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (blower == null || blower.isRemoved()) {
            blower = getBlower();
        }
        if (blower != null) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, CCBBlocks.END_CASING_BLOCK.get().defaultBlockState());
    }

    private @Nullable EndIncinerationBlowerBlockEntity getBlower() {
        if (level == null || !(level.getBlockEntity(worldPosition.above()) instanceof EndIncinerationBlowerBlockEntity be)) {
            return null;
        }

        return be;
    }

    public ScrollOptionBehaviour<BlowerWorkingMode> getBlowerWorkingMode() {
        return blowerWorkingMode;
    }

    public enum BlowerWorkingMode implements INamedIconOptions {
        SMOKING(CCBIcons.I_SMOKING),
        BLASTING(CCBIcons.I_BLASTING),
        IGNITION(CCBIcons.I_IGNITION);

        private final String translationKey;
        private final CCBIcons icon;

        BlowerWorkingMode(CCBIcons icon) {
            this.icon = icon;
            translationKey = "createcraftedbeginning.gui.end_incineration_blower.working_mode." + Lang.asId(name());
        }

        @Override
        public CCBIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}
