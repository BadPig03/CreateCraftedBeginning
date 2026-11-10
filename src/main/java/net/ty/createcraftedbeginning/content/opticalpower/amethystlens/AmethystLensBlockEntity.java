package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLensBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private AmethystLensFocusBehaviour focus;

    public AmethystLensBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        focus = new AmethystLensFocusBehaviour(this);
        behaviours.add(focus);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
        AmethystLensDisplay.appendGoggleTooltip(tooltip, getBlockState().getBlock().getName(), getFocalLength());
        return true;
    }

    int getFocalLength() {
        return focus.getValue();
    }
}
