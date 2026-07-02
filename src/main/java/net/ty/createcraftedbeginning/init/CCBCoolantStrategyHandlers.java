package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.BlueIceCoolantStrategy;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.BreezeCoolerCoolantStrategy;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.CoolantStrategyHandler;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.IceCoolantStrategy;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.PackedIceCoolantStrategy;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.PowderSnowCoolantStrategy;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBCoolantStrategyHandlers {
    public static void register() {
        SimpleRegistry<Block, CoolantStrategyHandler> registry = CoolantStrategyHandler.REGISTRY;
        CoolantStrategyHandler ice = new IceCoolantStrategy();

        registry.register(Blocks.ICE, ice);
        registry.register(Blocks.FROSTED_ICE, ice);
        registry.register(Blocks.PACKED_ICE, new PackedIceCoolantStrategy());
        registry.register(Blocks.BLUE_ICE, new BlueIceCoolantStrategy());
        registry.register(Blocks.POWDER_SNOW, new PowderSnowCoolantStrategy());
        registry.register(CCBBlocks.BREEZE_COOLER_BLOCK.get(), new BreezeCoolerCoolantStrategy());
    }
}