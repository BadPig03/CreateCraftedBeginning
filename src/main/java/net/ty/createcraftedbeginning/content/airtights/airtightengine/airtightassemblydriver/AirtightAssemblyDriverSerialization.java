package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightAssemblyDriverSerialization {
    private static final String COMPOUND_KEY_FLOW_METER = "FlowMeter";
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";
    private static final String COMPOUND_KEY_LEVEL_CALCULATOR = "LevelCalculator";
    private static final String COMPOUND_KEY_RESIDUE_MANAGER = "ResidueManager";

    private final AirtightAssemblyDriverCore driverCore;

    AirtightAssemblyDriverSerialization(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    CompoundTag write(Provider provider, boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        CCBNbtUtils.putTag(tag, COMPOUND_KEY_FLOW_METER, driverCore.getFlowMeter().write(provider, clientPacket));
        CCBNbtUtils.putTag(tag, COMPOUND_KEY_LEVEL_CALCULATOR, driverCore.getLevelCalculator().write(clientPacket));
        if (clientPacket) {
            CCBNbtUtils.putTag(tag, COMPOUND_KEY_STRUCTURE_MANAGER, driverCore.getStructureManager().writeClient());
        }
        else {
            CCBNbtUtils.putTag(tag, COMPOUND_KEY_RESIDUE_MANAGER, driverCore.getResidueManager().writePersistent());
        }
        return tag;
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            readClient(compoundTag, provider);
        }
        else {
            readPersistent(compoundTag, provider);
        }
        driverCore.getController().onReadComplete();
    }

    private void readClient(CompoundTag compoundTag, Provider provider) {
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_FLOW_METER)) {
            driverCore.getFlowMeter().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_FLOW_METER), provider, true);
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER)) {
            driverCore.getStructureManager().readClient(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER));
        }
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_LEVEL_CALCULATOR)) {
            return;
        }

        driverCore.getLevelCalculator().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_LEVEL_CALCULATOR), true);
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        CompoundTag levelTag = CCBNbtUtils.getCompoundOrEmpty(compoundTag, COMPOUND_KEY_LEVEL_CALCULATOR);
        driverCore.getLevelCalculator().read(levelTag, false);
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_FLOW_METER)) {
            driverCore.getFlowMeter().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            driverCore.getFlowMeter().loadEmptyState();
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_RESIDUE_MANAGER)) {
            driverCore.getResidueManager().readPersistent(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_RESIDUE_MANAGER));
        }
        else {
            driverCore.getResidueManager().loadEmptyPersistentState();
        }

        driverCore.getStructureManager().invalidateForServerLoad();
        driverCore.getController().onPersistentLoaded();
    }
}
