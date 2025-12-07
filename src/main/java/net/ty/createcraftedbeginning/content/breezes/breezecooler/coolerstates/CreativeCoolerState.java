package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class CreativeCoolerState extends BaseCoolerState {
    private static final String COMPOUND_KEY_CREATIVE_TYPE = "CreativeType";

    private CoolantType creativeType;

    public CreativeCoolerState(@NotNull CoolantType type) {
        super(switch (type) {
            case NONE -> 0;
            case NORMAL -> BreezeCoolerBlockEntity.MAX_COOLANT_CAPACITY;
        }, true);
        creativeType = type;
    }

    @Contract(pure = true)
    public static CoolantType getNextCoolantType(@NotNull CoolantType coolantTypeType) {
        return switch (coolantTypeType) {
            case NORMAL -> CoolantType.NONE;
            case NONE -> CoolantType.NORMAL;
        };
    }

    @Override
    public void tick(BreezeCoolerBlockEntity cooler) {
        super.tick(cooler);

        Level level = cooler.getLevel();
        if (level == null || level.getGameTime() % NOTIFY_INTERVAL != 0) {
            return;
        }

        cooler.notifyUpdate();
    }

    @Override
    public void read(@NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_CREATIVE_TYPE)) {
            creativeType = CoolantType.values()[compoundTag.getInt(COMPOUND_KEY_CREATIVE_TYPE)];
        }
        super.read(compoundTag);
    }

    @Override
    public void save(@NotNull CompoundTag compoundTag) {
        compoundTag.putInt(COMPOUND_KEY_CREATIVE_TYPE, creativeType.ordinal());
        super.save(compoundTag);
    }

    @Override
    public FrostLevel getFrostLevel() {
        return switch (creativeType) {
            case NORMAL -> FrostLevel.CHILLED;
            case NONE -> FrostLevel.RIMING;
        };
    }

    @Override
    public CoolantType getCoolantType() {
        return creativeType;
    }

    @Override
    public InteractionResult onItemInsert(BreezeCoolerBlockEntity cooler, @NotNull ItemStack stack, boolean forceOverflow, boolean simulate) {
        if (stack.getItem() != CCBItems.CREATIVE_ICE_CREAM.asItem()) {
            return InteractionResult.PASS;
        }

        if (!simulate) {
            CoolantType coolantType = getNextCoolantType(creativeType);
            cooler.setCoolerState(coolantType == CoolantType.NONE ? new InactiveCoolerState() : new CreativeCoolerState(coolantType));
            cooler.spawnParticleBurst();
            cooler.playSound();
        }
        return InteractionResult.SUCCESS;
    }
}
