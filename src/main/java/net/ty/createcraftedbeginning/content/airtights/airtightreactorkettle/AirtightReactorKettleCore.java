package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleCore {
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleStructureManager structureManager;
    private final AirtightReactorKettleTooltipBuilder tooltipBuilder;

    AirtightReactorKettleCore(AirtightReactorKettleBlockEntity kettle) {
        this.kettle = kettle;
        structureManager = new AirtightReactorKettleStructureManager(kettle);
        tooltipBuilder = new AirtightReactorKettleTooltipBuilder(this, kettle);
    }

    @Internal
    public AirtightReactorKettleStructureManager getStructureManager() {
        return structureManager;
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
        compoundTag.put(COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        return compoundTag;
    }

    void read(CompoundTag compoundTag) {
        structureManager.read(compoundTag.getCompound(COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    AirtightReactorKettleTooltipBuilder getTooltipBuilder() {
        return tooltipBuilder;
    }
}
