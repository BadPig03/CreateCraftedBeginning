package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel.AmethystCollectorPanelOutput.Limitation;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystCollectorPanelBlockEntity extends BlockEntity implements IHaveGoggleInformation, IHaveHoveringInformation {
    private static final long TOOLTIP_RETRY_DELAY_NS = 1000000000L;
    private static final AmethystCollectorPanelOutput TOOLTIP_UNAVAILABLE = new AmethystCollectorPanelOutput(0, Limitation.UNAVAILABLE);
    private long tooltipRetryAtNs = Long.MIN_VALUE;
    private @Nullable UUID pendingPlacer;

    public AmethystCollectorPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return addToGoggleTooltip(tooltip, getTooltipOutput());
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return addToTooltip(tooltip, getTooltipOutput());
    }

    @Internal
    public boolean addToGoggleTooltip(List<Component> tooltip, @Nullable AmethystCollectorPanelOutput output) {
        if (output == null) {
            return false;
        }

        AmethystCollectorPanelTooltip.addToGoggleTooltip(tooltip, output);
        return true;
    }

    @Internal
    public boolean addToTooltip(List<Component> tooltip, @Nullable AmethystCollectorPanelOutput output) {
        return output != null && AmethystCollectorPanelTooltip.addToTooltip(tooltip, output);
    }

    @Internal
    public @Nullable AmethystCollectorPanelOutput getTooltipOutput() {
        Level level = getLevel();
        if (level == null || isRemoved()) {
            return null;
        }

        long retryAtNs = tooltipRetryAtNs;
        if (retryAtNs != Long.MIN_VALUE && System.nanoTime() - retryAtNs < 0) {
            return TOOLTIP_UNAVAILABLE;
        }

        try {
            AmethystCollectorPanelOutput output = getOutput();
            tooltipRetryAtNs = Long.MIN_VALUE;
            return output;
        }
        catch (RuntimeException failure) {
            if (retryAtNs == Long.MIN_VALUE) {
                CCBAPI.LOGGER.warn("Failed to sample the amethyst collector panel tooltip at {} in dimension '{}'; showing an unavailable status.", worldPosition, level.dimension().location(), failure);
            }
            tooltipRetryAtNs = System.nanoTime() + TOOLTIP_RETRY_DELAY_NS;
            return TOOLTIP_UNAVAILABLE;
        }
    }

    @Internal
    public @Nullable AmethystCollectorPanelOutput getOutput() {
        Level level = getLevel();
        if (level == null || isRemoved()) {
            return null;
        }

        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.scanGeometry(level, worldPosition);
        AmethystCollectorPanelRectangle rectangle = geometry.activeRectangle();
        if (!geometry.topologyValid() || rectangle == null) {
            return new AmethystCollectorPanelOutput(0, Limitation.OVERSIZED);
        }

        return AmethystCollectorPanelPower.calculateOutput(level, rectangle);
    }

    void place(ServerLevel level, UUID placer) {
        pendingPlacer = placer;
        if (level.captureBlockSnapshots) {
            return;
        }

        commitPlacement(level);
    }

    void commitPlacement(ServerLevel level) {
        UUID placer = pendingPlacer;
        pendingPlacer = null;
        if (placer == null || !(level.getPlayerByUUID(placer) instanceof ServerPlayer player)) {
            return;
        }

        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.scanGeometry(level, worldPosition);
        AmethystCollectorPanelRectangle rectangle = geometry.activeRectangle();
        int maxSide = AmethystCollectorPanelBlock.MAX_SIDE;
        if (!geometry.topologyValid() || rectangle == null || rectangle.width() != maxSide || rectangle.depth() != maxSide) {
            return;
        }

        CCBAdvancements.LET_THERE_BE_LIGHT.awardTo(player);
    }
}
