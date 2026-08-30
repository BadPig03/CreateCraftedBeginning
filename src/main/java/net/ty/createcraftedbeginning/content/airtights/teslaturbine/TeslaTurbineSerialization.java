package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_FLOW_METER, core.getFlowMeter().write(provider, clientPacket));
        if (!clientPacket) {
            return compoundTag;
        }

        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_LEVEL_CALCULATOR, core.getLevelCalculator().write());
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER, core.getStructureManager().writeClient());
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
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_FLOW_METER)) {
            core.getFlowMeter().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_FLOW_METER), provider, true);
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_LEVEL_CALCULATOR)) {
            core.getLevelCalculator().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_LEVEL_CALCULATOR), true);
        }
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER)) {
            return;
        }

        core.getStructureManager().readClient(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    private void readPersistent(CompoundTag compoundTag, Provider provider) {
        core.getLevelCalculator().read(new CompoundTag(), false);
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_FLOW_METER)) {
            core.getFlowMeter().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_FLOW_METER), provider, false);
        }
        else {
            core.getFlowMeter().loadEmptyState();
        }
        core.getStructureManager().invalidateForServerLoad();
    }
}
