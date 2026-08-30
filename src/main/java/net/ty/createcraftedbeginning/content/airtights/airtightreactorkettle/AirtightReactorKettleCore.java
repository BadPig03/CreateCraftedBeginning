package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightReactorKettleCore {
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleStructureManager structureManager;
    private final AirtightReactorKettleTooltipBuilder tooltipBuilder;

    AirtightReactorKettleCore(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
        structureManager = new AirtightReactorKettleStructureManager(kettle);
        tooltipBuilder = new AirtightReactorKettleTooltipBuilder(this, kettle);
    }

    void lazyTick() {
        Level level = kettle.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick();
    }

    CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        return compoundTag;
    }

    void read(CompoundTag compoundTag) {
        structureManager.read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    AirtightReactorKettleStructureManager getStructureManager() {
        return structureManager;
    }

    AirtightReactorKettleTooltipBuilder getTooltipBuilder() {
        return tooltipBuilder;
    }
}
