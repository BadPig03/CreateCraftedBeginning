package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightForgingPressCore {
    private static final String COMPOUND_KEY_STRUCTURE_MANAGER = "StructureManager";

    private final AirtightForgingPressBlockEntity press;
    private final AirtightForgingPressStructureManager structureManager;
    private final AirtightForgingPressTooltipBuilder tooltipBuilder;

    public AirtightForgingPressCore(AirtightForgingPressBlockEntity press) {
        this.press = press;
        structureManager = new AirtightForgingPressStructureManager(press);
        tooltipBuilder = new AirtightForgingPressTooltipBuilder(this, press);
    }

    public void lazyTick() {
        Level level = press.getLevel();
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

    public AirtightForgingPressStructureManager getStructureManager() {
        return structureManager;
    }

    public AirtightForgingPressTooltipBuilder getTooltipBuilder() {
        return tooltipBuilder;
    }
}
