package net.ty.createcraftedbeginning.api.gas.effecthandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.packets.GasAreaOutlinePacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface AirtightPipeEffectHandler {
    SimpleRegistry<Gas, AirtightPipeEffectHandler> REGISTRY = SimpleRegistry.create();

    default void showOutline(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CatnipServices.NETWORK.sendToClientsAround(serverLevel, pos, 64, new GasAreaOutlinePacket(pos, direction, inflation, color));
    }

    void apply(Level level, BlockPos pos, Direction direction, Gas gasType);
}
