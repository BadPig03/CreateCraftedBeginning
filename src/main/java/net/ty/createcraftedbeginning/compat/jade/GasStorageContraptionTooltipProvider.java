package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.compat.jade.gas.GasStorageDataProvider;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageAccess;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum GasStorageContraptionTooltipProvider implements IServerDataProvider<EntityAccessor>, IComponentProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_STORAGE_CONTRAPTION_TOOLTIP;
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, EntityAccessor entityAccessor) {
        if (!(entityAccessor.getEntity() instanceof AbstractContraptionEntity contraption) || !(contraption.getContraption().getStorage() instanceof MountedGasStorageAccess gasStorage)) {
            return;
        }

        GasStorageDataProvider.readData(compoundTag, new HashSet<>(List.of(gasStorage.ccb$getGasStorage())), JadePlugin.GAS_STORAGE_CONTRAPTION_TOOLTIP, false);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag serverData = accessor.getServerData();
        if (!serverData.contains(GasStorageDataProvider.STORAGE_KEY) || !serverData.contains(GasStorageDataProvider.STORAGE_UID_KEY) || !JadePlugin.GAS_STORAGE_CONTRAPTION_TOOLTIP.toString().equals(serverData.getString(GasStorageDataProvider.STORAGE_UID_KEY))) {
            return;
        }

        GasStorageDataProvider.appendData(tooltip, serverData, accessor.showDetails(), false, false);
    }
}
