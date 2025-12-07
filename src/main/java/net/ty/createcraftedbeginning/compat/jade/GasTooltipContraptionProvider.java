package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.compat.jade.gas.DataProviderHelper;
import net.ty.createcraftedbeginning.compat.jade.gas.GasConstants;
import net.ty.createcraftedbeginning.mixin.MountedStorageManagerAccessor;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum GasTooltipContraptionProvider implements IServerDataProvider<EntityAccessor>, IComponentProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_CONTRAPTION_TOOLTIP;
    }

    @Override
    public void appendServerData(CompoundTag data, @NotNull EntityAccessor entityAccessor) {
        if (!(entityAccessor.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }
        if (!(entity.getContraption().getStorage() instanceof MountedStorageManagerAccessor accessor)) {
            return;
        }

        DataProviderHelper.getDataFromIGasHandler(data, accessor.getGases(), JadePlugin.GAS_CONTRAPTION_TOOLTIP, false);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, @NotNull EntityAccessor accessor, IPluginConfig config) {
        CompoundTag compoundTag = accessor.getServerData();
        if (!compoundTag.contains(GasConstants.STORAGE_KEY) || !compoundTag.contains(GasConstants.STORAGE_UID_KEY)) {
            return;
        }
        if (!JadePlugin.GAS_CONTRAPTION_TOOLTIP.toString().equals(compoundTag.getString(GasConstants.STORAGE_UID_KEY))) {
            return;
        }

        DataProviderHelper.appendData(tooltip, compoundTag, accessor.showDetails());
    }
}