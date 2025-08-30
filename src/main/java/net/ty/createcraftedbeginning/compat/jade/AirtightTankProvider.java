package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.content.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtightengine.GasControllerData;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.util.JadeForgeUtils;

import java.util.ArrayList;
import java.util.List;

public enum AirtightTankProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return CCBBlocks.AIRTIGHT_TANK_BLOCK.getId();
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof AirtightTankBlockEntity entity)) {
			return null;
		}

        AirtightTankBlockEntity controller = entity.getControllerBE();
        if (controller == null) {
            return null;
        }

        GasControllerData gasController = controller.gasController;
        if (!gasController.isActive()) {
            return new ArrayList<>(JadeForgeUtils.fromFluidHandler(controller.getFluidCapability()));
        }
        return new ArrayList<>();
    }
}
