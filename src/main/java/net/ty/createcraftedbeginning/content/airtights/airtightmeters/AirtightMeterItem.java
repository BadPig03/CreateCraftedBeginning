package net.ty.createcraftedbeginning.content.airtights.airtightmeters;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightMeterItem extends Item {
    public AirtightMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof AirtightTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        boolean changed = tank.hasTankGauge() ? tank.removeTankGauge() : tank.installTankGauge();
        if (!changed) {
            return InteractionResult.PASS;
        }

        if (!tank.hasTankGauge()) {
            return InteractionResult.sidedSuccess(false);
        }

        CCBAdvancements.VISUAL_MONITORING.awardTo(context.getPlayer());
        return InteractionResult.sidedSuccess(false);
    }
}
