package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity;
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

public enum BreezeChamberProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return CCBBlocks.BREEZE_CHAMBER_BLOCK.getId();
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof BreezeChamberBlockEntity entity)) {
			return null;
		}

        IFluidHandler fluidHandler = entity.getIFluidHandler();

        return new ArrayList<>(JadeForgeUtils.fromFluidHandler(fluidHandler));
    }
}