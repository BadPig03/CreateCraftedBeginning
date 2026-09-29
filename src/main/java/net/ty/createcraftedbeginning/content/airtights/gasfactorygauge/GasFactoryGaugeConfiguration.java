package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFactoryGaugeConfiguration {
    private GasFactoryGaugeConfiguration() {
    }

    public static void sanitizeInputs(FactoryPanelBlockEntity panel, Map<FactoryPanelPosition, Integer> amounts) {
        Level level = panel.getLevel();
        if (level == null || amounts.isEmpty()) {
            return;
        }

        for (Entry<FactoryPanelPosition, Integer> entry : amounts.entrySet()) {
            FactoryPanelBehaviour source = FactoryPanelBehaviour.at(level, entry.getKey());
            if (source == null || !VirtualGasItems.isVirtualItem(source.getFilter())) {
                continue;
            }

            entry.setValue(Mth.clamp(entry.getValue(), 0, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT));
        }
    }

    public static int getOutputAmount(FactoryPanelBlockEntity panel, FactoryPanelPosition position, int amount) {
        if (!(panel.panels.get(position.slot()) instanceof GasFactoryGaugeBehaviour)) {
            return amount;
        }

        return Mth.clamp(amount, 1, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT);
    }

    public static void finish(FactoryPanelBlockEntity panel, FactoryPanelPosition position) {
        if (!(panel.panels.get(position.slot()) instanceof GasFactoryGaugeBehaviour gauge)) {
            return;
        }

        gauge.activeCraftingArrangement = List.of();
    }
}
