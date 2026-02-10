package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlock.CanisterType;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightHatchEvents {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useOnItemHatchIgnoresSneak(@NotNull RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown()) {
            return;
        }

        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.is(CCBBlocks.AIRTIGHT_HATCH_BLOCK)) {
            return;
        }

        if (blockState.getValue(AirtightHatchBlock.CANISTER_TYPE) != CanisterType.EMPTY) {
            if (event.getUseItem() == TriState.DEFAULT) {
                event.setUseItem(TriState.FALSE);
                event.setUseBlock(TriState.TRUE);
            }
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof AirtightHatchBlockEntity hatch)) {
            return;
        }

        if (hatch.getTargetGasHandler(level) == null) {
            if (event.getUseItem() == TriState.DEFAULT) {
                event.setUseItem(TriState.FALSE);
                event.setUseBlock(TriState.TRUE);
            }
            return;
        }

        Direction facing = blockState.getValue(AirtightHatchBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof IAirtightComponent airtightComponent && !airtightComponent.isAirtight(targetPos, targetState, facing.getOpposite())) {
            if (event.getUseItem() == TriState.DEFAULT) {
                event.setUseItem(TriState.FALSE);
                event.setUseBlock(TriState.TRUE);
            }
            return;
        }

        if (!(event.getItemStack().getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents)) {
            return;
        }
        if (event.getUseItem() != TriState.DEFAULT) {
            return;
        }

        event.setUseItem(TriState.FALSE);
        event.setUseBlock(TriState.TRUE);
    }
}
