package net.ty.createcraftedbeginning.compat.jade.gas;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.mixin.MountedStorageManagerAccessor;
import org.jetbrains.annotations.NotNull;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

public class GasContraptionDataProvider implements IServerDataProvider<EntityAccessor> {
    public static final GasContraptionDataProvider INSTANCE = new GasContraptionDataProvider();

    @Override
    public ResourceLocation getUid() {
        return GasConstants.CONTRAPTION_DATA_PROVIDER;
    }

    @Override
    public void appendServerData(CompoundTag data, @NotNull EntityAccessor entityAccessor) {
        if (!(entityAccessor.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }
        if (!(entity.getContraption().getStorage() instanceof MountedStorageManagerAccessor accessor)) {
            return;
        }

        DataProviderHelper.getDataFromIGasHandler(data, accessor.getGases(), GasConstants.CONTRAPTION_TOOLTIP_RENDERER, false);
    }
}