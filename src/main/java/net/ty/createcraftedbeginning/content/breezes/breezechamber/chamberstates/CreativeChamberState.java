package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CreativeChamberState extends BaseChamberState {
    private static final String COMPOUND_KEY_CREATIVE_TYPE = "CreativeType";

    private ChargerType creativeType;
    private int energizationTimer;

    public CreativeChamberState(@NotNull ChargerType type) {
        super(switch (type) {
            case BAD -> -BreezeChamberBlockEntity.MAX_WIND_CAPACITY;
            case NONE -> 0;
            case NORMAL -> BreezeChamberBlockEntity.MAX_WIND_CAPACITY;
        }, true);
        creativeType = type;
    }

    @Contract(pure = true)
    public static ChargerType getNextChargeType(@NotNull ChargerType chargerType) {
        return switch (chargerType) {
            case NORMAL -> ChargerType.BAD;
            case BAD -> ChargerType.NONE;
            case NONE -> ChargerType.NORMAL;
        };
    }

    @Override
    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_CREATIVE_TYPE)) {
            creativeType = ChargerType.values()[compoundTag.getInt(COMPOUND_KEY_CREATIVE_TYPE)];
        }
        super.read(compoundTag);
    }

    @Override
    public void save(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_CREATIVE_TYPE, creativeType.ordinal());
        super.save(compoundTag);
    }

    @Override
    public void tick(@NotNull BreezeChamberBlockEntity chamber) {
        super.tick(chamber);
        Level level = chamber.getLevel();
        if (level == null || level.getGameTime() % 5 != 0) {
            return;
        }

        energizationTick(chamber);
        chamber.notifyUpdate();
    }

    public void energizationTick(@NotNull BreezeChamberBlockEntity chamber) {
        if (chamber.isControllerActive()) {
            return;
        }

        energizationTimer++;
        if (energizationTimer < NOTIFY_INTERVAL) {
            return;
        }

        chamber.doEnergization();
        energizationTimer = 0;
    }

    @Override
    public WindLevel getWindLevel() {
        return switch (creativeType) {
            case NORMAL -> WindLevel.GALE;
            case BAD -> WindLevel.ILL;
            case NONE -> WindLevel.CALM;
        };
    }

    @Override
    public ChargerType getChargerType() {
        return creativeType;
    }

    @Override
    public InteractionResult onItemInsert(BreezeChamberBlockEntity chamber, @NotNull ItemStack stack, boolean forceOverflow, boolean simulate) {
        if (stack.getItem() != CCBItems.CREATIVE_ICE_CREAM.asItem()) {
            return InteractionResult.PASS;
        }

        if (!simulate) {
            ChargerType chargerType = getNextChargeType(creativeType);
            chamber.setChamberState(chargerType == ChargerType.NONE ? new InactiveChamberState() : new CreativeChamberState(chargerType));
            chamber.spawnParticleBurst(true);
            chamber.playSound(chargerType == ChargerType.BAD);
        }
        return InteractionResult.SUCCESS;
    }
}
