package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeChamberState extends BaseChamberState {
    private final ChargerType creativeType;

    public CreativeChamberState(ChargerType chargerType) {
        super(switch (chargerType) {
            case BAD -> -BreezeChamberBlockEntity.getMaxWindCapacity();
            case NONE -> 0;
            case NORMAL -> BreezeChamberBlockEntity.getMaxWindCapacity();
        }, true);
        creativeType = chargerType;
    }

    @Contract(pure = true)
    static ChargerType getNextChargeType(ChargerType chargerType) {
        return switch (chargerType) {
            case NORMAL -> ChargerType.BAD;
            case BAD -> ChargerType.NONE;
            case NONE -> ChargerType.NORMAL;
        };
    }

    @Override
    public void tick(BreezeChamberBlockEntity chamber) {
        chamber.tickGasProcessing(creativeType, remainingTime);
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
}
