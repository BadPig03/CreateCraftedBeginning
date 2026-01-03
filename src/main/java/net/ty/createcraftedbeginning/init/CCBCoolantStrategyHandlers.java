package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.BreezeCoolerCoolantStrategy;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.CoolantStrategyHandler;
import net.ty.createcraftedbeginning.api.gas.coolantstrategy.DowngradeCoolantStrategy;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity.CoolantEfficiency;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class CCBCoolantStrategyHandlers {
    public static void register() {
        CoolantStrategyHandler downgrade = new DowngradeCoolantStrategy();
        SimpleRegistry<Block, CoolantStrategyHandler> registry = CoolantStrategyHandler.REGISTRY;

        registry.register(Blocks.ICE, (l, p, s) -> CoolantEfficiency.BASIC);
        registry.register(Blocks.FROSTED_ICE, (l, p, s) -> CoolantEfficiency.BASIC);
        registry.register(Blocks.PACKED_ICE, downgrade);
        registry.register(Blocks.BLUE_ICE, downgrade);
        registry.register(Blocks.POWDER_SNOW, (l, p, s) -> CoolantEfficiency.ADVANCED);
        registry.register(CCBBlocks.BREEZE_COOLER_BLOCK.get(), new BreezeCoolerCoolantStrategy());
    }
}