package net.ty.createcraftedbeginning.api.drainagehandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.packets.GasAreaOutlinePacket;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightDrainageHandler {
    SimpleRegistry<Gas, AirtightDrainageHandler> REGISTRY = SimpleRegistry.create();

    float getInflation();

    void apply(Level level, BlockPos pos, Direction direction, Gas gasType);

    default void showOutline(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (level.getGameTime() % 20 == 10) {
            CCBSoundEvents.GAS_DRAINAGE.playOnServer(level, pos, 1, 1);
        }
        CatnipServices.NETWORK.sendToClientsAround(serverLevel, pos, 64, new GasAreaOutlinePacket(pos, direction, inflation, color));
    }
}
