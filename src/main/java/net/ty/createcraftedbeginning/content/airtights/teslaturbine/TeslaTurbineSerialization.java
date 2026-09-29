package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class TeslaTurbineSerialization {
    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final TeslaTurbineCore core;

    TeslaTurbineSerialization(TeslaTurbineCore core) {
        this.core = core;
    }

    CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_FLOW_METER, core.getFlowMeter().write(provider, clientPacket));
        if (!clientPacket) {
            return compoundTag;
        }

        compoundTag.put(COMPOUND_KEY_LEVEL_CALCULATOR, core.getLevelCalculator().write());
        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, core.getStructureManager().writeClient());
        return compoundTag;
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(compoundTag, provider);
        }
        else {
            readPersistent(compoundTag, provider);
        }
        core.getController().onReadComplete();
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            core.getFlowMeter().read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider, true);
        }
        if (compoundTag.contains(COMPOUND_KEY_LEVEL_CALCULATOR)) {
            core.getLevelCalculator().read(compoundTag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR), true);
        }
        if (!compoundTag.contains(COMPOUND_KEY_STRUCTURE_MANAGER)) {
            return;
        }

        core.getStructureManager().readClient(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        core.getLevelCalculator().read(new CompoundTag(), false);
        if (compoundTag.contains(COMPOUND_KEY_FLOW_METER)) {
            core.getFlowMeter().read(compoundTag.getCompound(COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            core.getFlowMeter().loadEmptyState();
        }
        core.getStructureManager().invalidateForServerLoad();
    }
}
