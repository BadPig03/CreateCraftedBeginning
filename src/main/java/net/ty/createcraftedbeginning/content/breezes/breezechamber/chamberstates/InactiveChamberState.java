package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class InactiveChamberState extends BaseChamberState {
    public InactiveChamberState() {
        super(0, false);
    }

    @Override
    public WindLevel getWindLevel() {
        return WindLevel.CALM;
    }

    @Override
    public ChargerType getChargerType() {
        return ChargerType.NONE;
    }
}
