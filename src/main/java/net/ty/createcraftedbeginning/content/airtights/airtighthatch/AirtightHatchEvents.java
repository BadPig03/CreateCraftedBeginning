package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gascanisters.AirtightHatchCanisters;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AirtightHatchEvents {
    private AirtightHatchEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void onRightClickBlock(RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() || event.getUseItem() != TriState.DEFAULT || event.getUseBlock() != TriState.DEFAULT) {
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof AirtightHatchBlock)) {
            return;
        }

        boolean isOccupied = level.getBlockEntity(event.getPos()) instanceof AirtightHatchBlockEntity hatch ? !hatch.isEmpty() : state.getValue(AirtightHatchBlock.CANISTER_TYPE) != CanisterType.EMPTY;
        boolean hasCanister = AirtightHatchCanisters.isCompatible(event.getItemStack());
        if (!isOccupied && !hasCanister) {
            return;
        }

        event.setUseItem(TriState.FALSE);
        event.setUseBlock(TriState.TRUE);
    }
}
