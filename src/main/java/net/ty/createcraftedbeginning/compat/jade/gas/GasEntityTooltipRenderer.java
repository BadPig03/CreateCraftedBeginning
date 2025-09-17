package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.Accessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class GasEntityTooltipRenderer<ACCESSOR extends Accessor<?>> implements IComponentProvider<ACCESSOR> {
    public static final GasEntityTooltipRenderer<?> INSTANCE = new GasEntityTooltipRenderer<>();

    @Override
    public ResourceLocation getUid() {
        return GasConstants.CONTRAPTION_TOOLTIP_RENDERER;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, @NotNull ACCESSOR accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(GasConstants.STORAGE_KEY) || !data.contains(GasConstants.STORAGE_UID_KEY)) {
            return;
        }

        String uidString = data.getString(GasConstants.STORAGE_UID_KEY);
        if (!GasConstants.CONTRAPTION_TOOLTIP_RENDERER.toString().equals(uidString)) {
            return;
        }

        boolean showDetails = accessor.showDetails();
        DataProviderHelper.appendData(tooltip, data, showDetails);
    }
}