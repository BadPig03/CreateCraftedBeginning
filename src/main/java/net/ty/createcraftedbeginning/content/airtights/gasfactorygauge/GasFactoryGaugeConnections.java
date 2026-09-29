package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFactoryGaugeConnections {
    private GasFactoryGaugeConnections() {
    }

    public static @Nullable Check check(@Nullable FactoryPanelBehaviour from, @Nullable FactoryPanelBehaviour to) {
        if (from == null || to == null) {
            return null;
        }

        BlockPos toPos = to.getPos();
        BlockPos fromPos = from.getPos();
        BlockState toState = to.getWorld().getBlockState(toPos);
        BlockState fromState = from.getWorld().getBlockState(fromPos);
        boolean toGas = toState.getBlock() instanceof GasFactoryGaugeBlock;
        boolean fromGas = fromState.getBlock() instanceof GasFactoryGaugeBlock;
        if (toGas == fromGas || !(toState.getBlock() instanceof FactoryPanelBlock) || !(fromState.getBlock() instanceof FactoryPanelBlock)) {
            return null;
        }

        if (from.targetedBy.containsKey(to.getPanelPosition())) {
            return new Check("factory_panel.already_connected");
        }

        if (from.targetedBy.size() >= 9) {
            return new Check("factory_panel.cannot_add_more_inputs");
        }

        if (toState.getValue(FactoryPanelBlock.FACE) != fromState.getValue(FactoryPanelBlock.FACE) || toState.getValue(FactoryPanelBlock.FACING) != fromState.getValue(FactoryPanelBlock.FACING)) {
            return new Check("factory_panel.same_orientation");
        }

        BlockPos diff = toPos.subtract(fromPos);
        if (FactoryPanelBlock.connectedDirection(toState).getAxis().choose(diff.getX(), diff.getY(), diff.getZ()) != 0) {
            return new Check("factory_panel.same_surface");
        }

        if (!diff.closerThan(BlockPos.ZERO, 16)) {
            return new Check("factory_panel.too_far_apart");
        }

        if (to.panelBE().restocker) {
            return new Check("factory_panel.input_in_restock_mode");
        }

        if (to.getFilter().isEmpty() || from.getFilter().isEmpty()) {
            return new Check("factory_panel.no_item");
        }

        return new Check(null);
    }

    public record Check(@Nullable String issue) {}
}
