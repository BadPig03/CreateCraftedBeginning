package net.ty.createcraftedbeginning.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.content.aircompressor.AirCompressorBlockEntity;
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

public enum AirCompressorProvider implements IServerExtensionProvider<CompoundTag>, IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return CCBBlocks.AIR_COMPRESSOR_BLOCK.getId();
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof AirCompressorBlockEntity entity)) {
			return null;
		}

        IFluidHandler inputFluidHandler = entity.getInputFluidHandler();
        var groups = new ArrayList<>(JadeForgeUtils.fromFluidHandler(inputFluidHandler));

        IFluidHandler outputFluidHandler = entity.getOutputFluidHandler();
        groups.addAll(JadeForgeUtils.fromFluidHandler(outputFluidHandler));

        return groups;
    }
}