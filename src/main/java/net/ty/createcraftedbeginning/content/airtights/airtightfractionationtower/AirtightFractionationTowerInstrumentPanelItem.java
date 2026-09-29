package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerStructure.AssemblyFailure;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightFractionationTowerInstrumentPanelItem extends Item {
    public AirtightFractionationTowerInstrumentPanelItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        AssemblyFailure failure = AirtightFractionationTowerStructure.assemble(level, context.getClickedPos(), player);
        if (failure != null) {
            if (failure.failed()) {
                return InteractionResult.FAIL;
            }

            if (player instanceof ServerPlayer serverPlayer) {
                CatnipServices.NETWORK.sendToClient(serverPlayer, new AirtightFractionationTowerFailurePacket(failure.min(), failure.max(), failure.reason()));
            }
            return InteractionResult.FAIL;
        }

        if (!player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
