package net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlock.WindLevel;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class IllChamberState extends BaseChamberState {
    public IllChamberState(int remainingTime, boolean isCreative) {
        super(remainingTime, isCreative);
    }

    @Override
    public void tick(BreezeChamberBlockEntity chamber) {
        Level level = chamber.getLevel();
        if (level == null || isCreative || remainingTime >= 0) {
            return;
        }

        remainingTime++;
        if (remainingTime >= 0) {
            chamber.setChamberState(new InactiveChamberState());
            return;
        }
        if (level.isClientSide) {
            return;
        }

        chamber.syncWindProgress();
        chamber.tickGasProcessing(ChargerType.BAD);
    }

    @Override
    public WindLevel getWindLevel() {
        return WindLevel.ILL;
    }

    @Override
    public ChargerType getChargerType() {
        return ChargerType.BAD;
    }

    @Override
    public InteractionResult onItemInsert(BreezeChamberBlockEntity chamber, ItemStack stack, boolean forceOverflow, boolean simulate) {
        return insertWindCharge(chamber, stack, forceOverflow, simulate, true);
    }
}
