package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightForgingPressCore {
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final AirtightForgingPressBlockEntity press;
    private final AirtightForgingPressStructureManager structureManager;
    private final AirtightForgingPressTooltipBuilder tooltipBuilder;

    AirtightForgingPressCore(AirtightForgingPressBlockEntity press) {
        this.press = press;
        structureManager = new AirtightForgingPressStructureManager(press);
        tooltipBuilder = new AirtightForgingPressTooltipBuilder(this, press);
    }

    void lazyTick() {
        Level level = press.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        structureManager.tick();
    }

    CompoundTag write() {
        CompoundTag coreTag = new CompoundTag();
        CCBNbtUtils.putTag(coreTag, COMPOUND_KEY_STRUCTURE_MANAGER, structureManager.write());
        return coreTag;
    }

    void read(CompoundTag compoundTag) {
        structureManager.read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_STRUCTURE_MANAGER));
    }

    AirtightForgingPressStructureManager getStructureManager() {
        return structureManager;
    }

    AirtightForgingPressTooltipBuilder getTooltipBuilder() {
        return tooltipBuilder;
    }
}
