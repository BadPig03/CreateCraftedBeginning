package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightAssemblyDriverSerialization {
    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_RESIDUE_MANAGER = "ResidueManager";

    private final AirtightAssemblyDriverCore driverCore;

    public AirtightAssemblyDriverSerialization(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    public CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.put(COMPOUND_KEY_FLOW_METER, driverCore.getFlowMeter().write(provider, clientPacket));
        tag.put(COMPOUND_KEY_LEVEL_CALCULATOR, driverCore.getLevelCalculator().write(clientPacket));
        if (clientPacket) {
            tag.put(COMPOUND_KEY_STRUCTURE_MANAGER, driverCore.getStructureManager().writeClient());
        }
        else {
            tag.put(COMPOUND_KEY_RESIDUE_MANAGER, driverCore.getResidueManager().writePersistent());
        }
        return tag;
    }

    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(tag, provider);
        }
        else {
            readPersistent(tag, provider);
        }
        driverCore.getController().onReadComplete();
    }

    private void readClient(CompoundTag tag, Provider provider) {
        if (tag.contains(COMPOUND_KEY_FLOW_METER)) {
            driverCore.getFlowMeter().read(tag.getCompound(COMPOUND_KEY_FLOW_METER), provider, true);
        }
        if (tag.contains(COMPOUND_KEY_STRUCTURE_MANAGER)) {
            driverCore.getStructureManager().readClient(tag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
        }
        if (!tag.contains(COMPOUND_KEY_LEVEL_CALCULATOR)) {
            return;
        }

        driverCore.getLevelCalculator().read(tag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR), true);
    }

    private void readPersistent(CompoundTag tag, Provider provider) {
        CompoundTag levelTag = tag.contains(COMPOUND_KEY_LEVEL_CALCULATOR) ? tag.getCompound(COMPOUND_KEY_LEVEL_CALCULATOR) : new CompoundTag();
        driverCore.getLevelCalculator().read(levelTag, false);
        if (tag.contains(COMPOUND_KEY_FLOW_METER)) {
            driverCore.getFlowMeter().read(tag.getCompound(COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            driverCore.getFlowMeter().loadEmptyState();
        }
        if (tag.contains(COMPOUND_KEY_RESIDUE_MANAGER)) {
            driverCore.getResidueManager().readPersistent(tag.getCompound(COMPOUND_KEY_RESIDUE_MANAGER));
        }
        else {
            driverCore.getResidueManager().loadEmptyPersistentState();
        }

        driverCore.getStructureManager().invalidateForServerLoad();
        driverCore.getController().onPersistentLoaded();
    }
}
