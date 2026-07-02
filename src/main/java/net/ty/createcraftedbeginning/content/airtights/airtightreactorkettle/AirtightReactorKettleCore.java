package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleCore {
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleStructureManager structureManager;
    private final AirtightReactorKettleTooltipBuilder tooltipBuilder;

    public AirtightReactorKettleCore(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
        structureManager = new AirtightReactorKettleStructureManager(kettle);
        tooltipBuilder = new AirtightReactorKettleTooltipBuilder(this, kettle);
    }

    public void lazyTick() {
        Level level = kettle.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick();
    }

    public CompoundTag write() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        return compoundTag;
    }

    public void read(CompoundTag compoundTag) {
        structureManager.read(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    public AirtightReactorKettleStructureManager getStructureManager() {
        return structureManager;
    }

    public AirtightReactorKettleTooltipBuilder getTooltipBuilder() {
        return tooltipBuilder;
    }
}
