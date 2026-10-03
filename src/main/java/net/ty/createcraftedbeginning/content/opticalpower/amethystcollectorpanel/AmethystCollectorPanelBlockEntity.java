package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystCollectorPanelBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public AmethystCollectorPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        AmethystCollectorPanelOutput output = getOutput();
        if (output == null) {
            return false;
        }

        AmethystCollectorPanelTooltip.addToGoggleTooltip(tooltip, output);
        return true;
    }

    @Internal
    public @Nullable AmethystCollectorPanelOutput getOutput() {
        Level level = getLevel();
        if (level == null || isRemoved()) {
            return null;
        }

        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.findGeometry(level, worldPosition);
        AmethystCollectorPanelRectangle rectangle = geometry.activeRectangle();
        if (!geometry.topologyValid() || rectangle == null) {
            return new AmethystCollectorPanelOutput(0, Limitation.OVERSIZED);
        }

        return AmethystCollectorPanelPower.calculateOutput(level, rectangle);
    }
}
