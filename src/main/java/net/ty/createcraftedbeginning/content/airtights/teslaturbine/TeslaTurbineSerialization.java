package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class TeslaTurbineSerialization {
    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final TeslaTurbineCore core;

    public TeslaTurbineSerialization(TeslaTurbineCore core) {
        this.core = core;
    }

    private static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key) ? tag.getCompound(key) : new CompoundTag();
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY_FLOW_METER, core.getFlowMeter().write(provider, clientPacket));
        if (!clientPacket) {
            return tag;
        }

        tag.put(COMPOUND_KEY_LEVEL_CALCULATOR, core.getLevelCalculator().write(true));
        tag.put(COMPOUND_KEY_STRUCTURE_MANAGER, core.getStructureManager().writeClient());
        return tag;
    }

    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(tag, provider);
        }
        else {
            readPersistent(tag, provider);
        }

        core.getController().onReadComplete();
    }

    private void readClient(CompoundTag tag, Provider provider) {
        core.getFlowMeter().read(getCompoundOrEmpty(tag, COMPOUND_KEY_FLOW_METER), provider, true);
        core.getLevelCalculator().read(getCompoundOrEmpty(tag, COMPOUND_KEY_LEVEL_CALCULATOR), true);
        core.getStructureManager().readClient(getCompoundOrEmpty(tag, COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    private void readPersistent(CompoundTag tag, Provider provider) {
        core.getLevelCalculator().read(new CompoundTag(), false);
        if (tag.contains(COMPOUND_KEY_FLOW_METER)) {
            core.getFlowMeter().read(tag.getCompound(COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            core.getFlowMeter().loadEmptyState();
        }
        core.getStructureManager().invalidateForServerLoad();
    }
}
