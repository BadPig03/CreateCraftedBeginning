package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeBlockEntity extends FactoryPanelBlockEntity {
    public GasFactoryGaugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        panels = new EnumMap<>(PanelSlot.class);
        redraw = true;
        for (PanelSlot slot : PanelSlot.values()) {
            GasFactoryGaugeBehaviour behaviour = new GasFactoryGaugeBehaviour(this, slot);
            panels.put(slot, behaviour);
            behaviours.add(behaviour);
        }

        advancements = new AdvancementBehaviour(this, AllAdvancements.FACTORY_GAUGE);
        behaviours.add(advancements);
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide() || activePanels() == 0 || !(getBlockState().getBlock() instanceof GasFactoryGaugeBlock)) {
            return;
        }

        Direction direction = FactoryPanelBlock.connectedDirection(getBlockState()).getOpposite();
        BlockPos attachedPos = worldPosition.relative(direction);
        if (!level.isLoaded(attachedPos)) {
            return;
        }

        boolean isPackagerAttached = level.getBlockEntity(attachedPos) instanceof GasPackagerBlockEntity;
        if (restocker == isPackagerAttached) {
            return;
        }

        restocker = isPackagerAttached;
        redraw = true;
        sendData();
    }

    @Override
    @Nullable
    public GasPackagerBlockEntity getRestockedPackager() {
        BlockState state = getBlockState();
        if (!restocker || !(state.getBlock() instanceof GasFactoryGaugeBlock)) {
            return null;
        }

        Direction direction = FactoryPanelBlock.connectedDirection(state).getOpposite();
        BlockPos packagerPos = worldPosition.relative(direction);
        if (level == null || !level.isLoaded(packagerPos)) {
            return null;
        }

        return level.getBlockEntity(packagerPos) instanceof GasPackagerBlockEntity packager ? packager : null;
    }

    @Override
    public void destroy() {
        forEachBehaviour(BlockEntityBehaviour::destroy);
        int panelCount = activePanels();
        if (panelCount <= 1 || level == null) {
            return;
        }

        Block.popResource(level, worldPosition, new ItemStack(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK, panelCount - 1));
    }

    @Override
    public boolean addPanel(PanelSlot slot, UUID frequency) {
        FactoryPanelBehaviour current = panels.get(slot);
        if (current == null || current.isActive() || current instanceof GasFactoryGaugeBehaviour) {
            return super.addPanel(slot, frequency);
        }

        GasFactoryGaugeBehaviour replacement = new GasFactoryGaugeBehaviour(this, slot);
        removeBehaviour(current.getType());
        attachBehaviourLate(replacement);
        panels.put(slot, replacement);
        return super.addPanel(slot, frequency);
    }
}
