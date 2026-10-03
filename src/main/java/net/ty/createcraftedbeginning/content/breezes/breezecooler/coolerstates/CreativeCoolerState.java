package net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeCoolerState extends BaseCoolerState {
    private final CoolantType creativeType;

    public CreativeCoolerState(CoolantType coolantType) {
        super(switch (coolantType) {
            case NONE -> 0;
            case NORMAL -> BreezeCoolerBlockEntity.getMaxCoolantCapacity();
        }, true);
        creativeType = coolantType;
    }

    @Contract(pure = true)
    public static CoolantType getNextCoolantType(CoolantType coolantType) {
        return switch (coolantType) {
            case NORMAL -> CoolantType.NONE;
            case NONE -> CoolantType.NORMAL;
        };
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
    public boolean onSnowballImpact(BreezeCoolerBlockEntity cooler) {
        return false;
    }
}
