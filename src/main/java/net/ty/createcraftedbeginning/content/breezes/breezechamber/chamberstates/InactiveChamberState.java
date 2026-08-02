package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
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

    @Override
    public InteractionResult onItemInsert(BreezeChamberBlockEntity chamber, ItemStack stack, boolean forceOverflow, boolean simulate) {
        return insertWindCharge(chamber, stack, forceOverflow, simulate);
    }
}
