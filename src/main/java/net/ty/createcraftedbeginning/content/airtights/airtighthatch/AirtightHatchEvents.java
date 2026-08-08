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
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class AirtightHatchEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(RightClickBlock event) {
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

        boolean isOccupied = state.getValue(AirtightHatchBlock.CANISTER_TYPE) != CanisterType.EMPTY;
        boolean hasCanister = event.getItemStack().getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents;
        if (!isOccupied && !hasCanister) {
            return;
        }

        event.setUseItem(TriState.FALSE);
        event.setUseBlock(TriState.TRUE);
    }
}
