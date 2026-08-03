package net.ty.createcraftedbeginning.compat.jade;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.compat.jade.gas.GasConstants;
import net.ty.createcraftedbeginning.compat.jade.gas.GasDataProvider;
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
public enum GasTooltipContraptionProvider implements IServerDataProvider<EntityAccessor>, IComponentProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return JadePlugin.GAS_CONTRAPTION_TOOLTIP;
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor entityAccessor) {
        if (!(entityAccessor.getEntity() instanceof AbstractContraptionEntity contraption)) {
            return;
        }

        if (!(contraption.getContraption().getStorage() instanceof IMountedStorageManagerWithGas gasStorage)) {
            return;
        }

        GasDataProvider.readData(data, new HashSet<>(List.of(gasStorage.ccb$getGases())), JadePlugin.GAS_CONTRAPTION_TOOLTIP, false);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(GasConstants.STORAGE_KEY) || !data.contains(GasConstants.STORAGE_UID_KEY)) {
            return;
        }

        if (!JadePlugin.GAS_CONTRAPTION_TOOLTIP.toString().equals(data.getString(GasConstants.STORAGE_UID_KEY))) {
            return;
        }

        GasDataProvider.appendData(tooltip, data, accessor.showDetails());
    }
}
