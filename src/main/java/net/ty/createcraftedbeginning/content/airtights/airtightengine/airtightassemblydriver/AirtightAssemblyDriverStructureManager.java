package net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverStructureScanner.ScanResult;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

import static net.ty.createcraftedbeginning.content.airtights.airtightengine.airtightassemblydriver.AirtightAssemblyDriverCore.MAX_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightAssemblyDriverStructureManager {
    private static final String COMPOUND_KEY_ATTACHED_ENGINES = "AttachedEngines";
    private static final String COMPOUND_KEY_ATTACHED_OUTLETS = "AttachedOutlets";
    private static final String COMPOUND_KEY_ATTACHED_CHAMBERS = "AttachedChambers";
    private static final String COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL = "AttachedWindChargingLevel";
    private static final String COMPOUND_KEY_STRUCTURE_VALID = "StructureValid";

    private static final int INITIAL_EVALUATION_DELAY = 2;
    private static final int INCOMPLETE_EVALUATION_RETRY_DELAY = 20;
    private static final AirtightAssemblyDriverStructureScanner scanner = new AirtightAssemblyDriverStructureScanner();

    private final AirtightAssemblyDriverCore driverCore;
    private boolean structureValid;
    private boolean evaluationRequired = true;
    private int evaluationCooldown;
    private int attachedChambers;
    private int attachedWindChargingLevel;
    private int attachedEngines;
    private int attachedOutlets;

    public AirtightAssemblyDriverStructureManager(AirtightAssemblyDriverCore driverCore) {
        this.driverCore = driverCore;
    }

    private static int getMaxAttachedSurfaceBlocks() {
        int width = AirtightTankBlockEntity.getConfiguredMaxWidth();
        int length = AirtightTankBlockEntity.getConfiguredMaxLength();
        return 2 * (width * width + 2 * width * length);
    }

    private static int getMaxAttachedChambers() {
        int width = AirtightTankBlockEntity.getConfiguredMaxWidth();
        int length = AirtightTankBlockEntity.getConfiguredMaxLength();
        return width * Math.max(width, length);
    }

    private static int getMaxAttachedWindChargingLevel() {
        return getMaxAttachedChambers() * MAX_LEVEL;
    }

    private static int readBoundedInt(CompoundTag tag, String key, int max) {
        return tag.contains(key) ? Mth.clamp(tag.getInt(key), 0, max) : 0;
    }

    public void tick(AirtightTankBlockEntity controller) {
        if (!evaluationRequired) {
            return;
        }

        if (evaluationCooldown > 0) {
            evaluationCooldown--;
            return;
        }

        evaluate(controller);
        if (evaluationRequired) {
            evaluationCooldown = INCOMPLETE_EVALUATION_RETRY_DELAY;
        }
    }

    public void requestEvaluation() {
        evaluationRequired = true;
        evaluationCooldown = 0;
    }

    public void evaluate(AirtightTankBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        DerivedState previous = captureDerivedState();
        evaluationRequired = false;
        evaluationCooldown = 0;

        ScanResult result = scanner.scan(controller, level);
        if (result.complete()) {
            applyScanResult(result);
            if (previous.attachedOutlets() > attachedOutlets) {
                driverCore.getResidueManager().applyRemovalPenalty(false);
            }
        }
        else {
            clearDerivedState();
            evaluationRequired = true;
        }

        driverCore.getResidueManager().updateOutletsPositions(result.complete() ? result.outletPositions() : Set.of());
        driverCore.getLevelCalculator().updateWindChargingLevel(attachedWindChargingLevel);
        if (!previous.matches(this)) {
            driverCore.markForClientSync();
        }
    }

    public void reset() {
        boolean changed = hasDerivedState();
        clearDerivedState();
        evaluationRequired = true;
        evaluationCooldown = 0;
        driverCore.getResidueManager().clearOutletsPositions();
        driverCore.getLevelCalculator().updateWindChargingLevel(0);
        if (changed) {
            driverCore.markForClientSync();
        }
    }

    public void invalidateForServerLoad() {
        clearDerivedState();
        evaluationRequired = true;
        evaluationCooldown = INITIAL_EVALUATION_DELAY;
        driverCore.getResidueManager().clearOutletsPositions();
        driverCore.getLevelCalculator().loadWindChargingLevel(0);
    }

    public boolean isEvaluationRequired() {
        return evaluationRequired;
    }

    public boolean isAssembled() {
        return attachedEngines > 0 && structureValid;
    }

    public boolean isActive() {
        return isAssembled() && attachedOutlets > 0 && attachedWindChargingLevel > 0;
    }

    public int getAttachedEngines() {
        return attachedEngines;
    }

    public int getAttachedOutlets() {
        return attachedOutlets;
    }

    public CompoundTag writeClient() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(COMPOUND_KEY_ATTACHED_ENGINES, attachedEngines);
        tag.putInt(COMPOUND_KEY_ATTACHED_OUTLETS, attachedOutlets);
        tag.putInt(COMPOUND_KEY_ATTACHED_CHAMBERS, attachedChambers);
        tag.putInt(COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL, attachedWindChargingLevel);
        tag.putBoolean(COMPOUND_KEY_STRUCTURE_VALID, structureValid);
        return tag;
    }

    public void readClient(CompoundTag tag) {
        int maxAttachedSurfaceBlocks = getMaxAttachedSurfaceBlocks();
        attachedEngines = readBoundedInt(tag, COMPOUND_KEY_ATTACHED_ENGINES, maxAttachedSurfaceBlocks);
        attachedOutlets = readBoundedInt(tag, COMPOUND_KEY_ATTACHED_OUTLETS, maxAttachedSurfaceBlocks);
        attachedChambers = readBoundedInt(tag, COMPOUND_KEY_ATTACHED_CHAMBERS, getMaxAttachedChambers());
        attachedWindChargingLevel = readBoundedInt(tag, COMPOUND_KEY_ATTACHED_WIND_CHARGING_LEVEL, getMaxAttachedWindChargingLevel());
        structureValid = tag.getBoolean(COMPOUND_KEY_STRUCTURE_VALID);
        evaluationRequired = false;
        evaluationCooldown = 0;
    }

    private void applyScanResult(ScanResult result) {
        attachedEngines = result.attachedEngines();
        attachedOutlets = result.attachedOutlets();
        attachedChambers = result.attachedChambers();
        attachedWindChargingLevel = result.attachedWindChargingLevel();
        structureValid = result.structureValid();
    }

    private boolean hasDerivedState() {
        return attachedEngines != 0 || attachedOutlets != 0 || attachedChambers != 0 || attachedWindChargingLevel != 0 || structureValid;
    }

    private void clearDerivedState() {
        attachedEngines = 0;
        attachedOutlets = 0;
        attachedChambers = 0;
        attachedWindChargingLevel = 0;
        structureValid = false;
    }

    private DerivedState captureDerivedState() {
        return new DerivedState(attachedEngines, attachedOutlets, attachedChambers, attachedWindChargingLevel, structureValid);
    }

    private record DerivedState(int attachedEngines, int attachedOutlets, int attachedChambers, int attachedWindChargingLevel, boolean structureValid) {
        boolean matches(AirtightAssemblyDriverStructureManager manager) {
            return attachedEngines == manager.attachedEngines && attachedOutlets == manager.attachedOutlets && attachedChambers == manager.attachedChambers && attachedWindChargingLevel == manager.attachedWindChargingLevel && structureValid == manager.structureValid;
        }
    }
}
