package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IGasTransporter;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPumpBlockEntity extends KineticBlockEntity implements IGasTransporter {
    private static final int LAZY_TICK_RATE = 10;

    private final AirtightPumpPressureController pressureController;
    private CCBAdvancementBehaviour advancementBehaviour;

    public AirtightPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        pressureController = new AirtightPumpPressureController(this);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    @Override
    public void tick() {
        if (level == null) {
            return;
        }

        pressureController.beforeTick();
        super.tick();
        pressureController.afterTick();
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (!pressureController.shouldHandleSpeedChange(previousSpeed)) {
            return;
        }

        if (pressureController.hasRequiredSpeed() && advancementBehaviour != null) {
            advancementBehaviour.awardPlayer(CCBAdvancements.TAKE_A_DEEP_BREATH);
        }
        pressureController.rebuildPressure();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.TAKE_A_DEEP_BREATH, CCBAdvancements.GASEOUS_VARIATIONS, CCBAdvancements.MINTY_FRESH);
        behaviours.add(advancementBehaviour);

        behaviours.add(new AirtightPumpTransportBehaviour(this));
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        pressureController.lazyTick();
    }

    public void updatePipesOnSide(Direction side) {
        pressureController.updatePipesOnSide(side);
    }

    public void markPressureUpdate() {
        pressureController.markPressureUpdate();
    }

    @Override
    public boolean canTransport(Level level, BlockState state, BlockPos pos, Direction direction) {
        return pressureController.canTransport(state, direction);
    }

    @Override
    public CCBAdvancementBehaviour getAdvancementBehaviour() {
        return advancementBehaviour;
    }

    boolean isPumpRunning() {
        return pressureController.isPumpRunning();
    }

    boolean isSideAccessible(Direction direction) {
        return pressureController.isSideAccessible(direction);
    }

    boolean isFront(Direction direction) {
        return pressureController.isFront(direction);
    }

    float getPumpPressure() {
        return pressureController.getPumpPressure();
    }
}
