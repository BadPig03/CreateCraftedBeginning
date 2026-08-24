package net.ty.createcraftedbeginning.compat.functionalstorage.registry;

import com.buuz135.functionalstorage.FunctionalStorage.DrawerType;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlock;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrate;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBFunctionalStorageBlocks {
    private static final CCBRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();

    static {
        CCB_REGISTRATE.setCreativeSection(CCBCreativeTabSection.AIRTIGHTS);
    }

    public static final BlockEntry<GasDrawerBlock> GAS_DRAWER_1_BLOCK = CCB_REGISTRATE.block("gas_drawer_1", properties -> new GasDrawerBlock(DrawerType.X_1, properties)).properties(ignoredProperties -> Properties.ofFullCopy(Blocks.STONE_BRICKS)).blockstate(NonNullBiConsumer.noop()).lang("Gas Drawer (1x1)").item().model(NonNullBiConsumer.noop()).build().register();
    public static final BlockEntry<GasDrawerBlock> GAS_DRAWER_2_BLOCK = CCB_REGISTRATE.block("gas_drawer_2", properties -> new GasDrawerBlock(DrawerType.X_2, properties)).properties(ignoredProperties -> Properties.ofFullCopy(Blocks.STONE_BRICKS)).blockstate(NonNullBiConsumer.noop()).lang("Gas Drawer (1x2)").item().model(NonNullBiConsumer.noop()).build().register();
    public static final BlockEntry<GasDrawerBlock> GAS_DRAWER_4_BLOCK = CCB_REGISTRATE.block("gas_drawer_4", properties -> new GasDrawerBlock(DrawerType.X_4, properties)).properties(ignoredProperties -> Properties.ofFullCopy(Blocks.STONE_BRICKS)).blockstate(NonNullBiConsumer.noop()).lang("Gas Drawer (2x2)").item().model(NonNullBiConsumer.noop()).build().register();

    public static void register() {
        CCBCreativeTabs.registerSectionTail(CCBCreativeTabSection.AIRTIGHTS, GAS_DRAWER_1_BLOCK, GAS_DRAWER_2_BLOCK, GAS_DRAWER_4_BLOCK);
    }
}
