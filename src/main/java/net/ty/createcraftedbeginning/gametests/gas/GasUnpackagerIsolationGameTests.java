package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasUnpackagerIsolationGameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);
    private static final BlockPos LINK = MACHINE.above();

    private GasUnpackagerIsolationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void blockRetainsFacingProperty(GameTestHelper helper) {
        BlockState state = CCBBlocks.GAS_UNPACKAGER_BLOCK.getDefaultState();
        helper.assertTrue(state.hasProperty(BlockStateProperties.FACING), "gas unpackager lost its facing property");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void adjacentRedstoneDoesNotActivateUnpackager(GameTestHelper helper) {
        helper.setBlock(MACHINE, CCBBlocks.GAS_UNPACKAGER_BLOCK.getDefaultState());
        helper.setBlock(MACHINE.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(2, () -> {
            GasUnpackagerBlockEntity unpackager = helper.getBlockEntity(MACHINE);
            helper.assertTrue(!unpackager.redstonePowered, "redstone activated the gas unpackager");
            helper.assertTrue(!unpackager.redstoneModeActive(), "gas unpackager reports a redstone packaging mode");
            unpackager.activate();
            helper.assertTrue(!unpackager.redstonePowered, "inherited activation enabled redstone packaging");
            helper.assertTrue(unpackager.getAvailableItems().isEmpty(), "gas unpackager advertised logistics inventory");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void stockLinkDoesNotRecognizeUnpackager(GameTestHelper helper) {
        helper.setBlock(MACHINE, CCBBlocks.GAS_UNPACKAGER_BLOCK.getDefaultState());
        helper.setBlock(LINK, AllBlocks.STOCK_LINK.getDefaultState().setValue(PackagerLinkBlock.FACE, AttachFace.FLOOR));
        helper.runAfterDelay(2, () -> {
            PackagerLinkBlockEntity link = helper.getBlockEntity(LINK);
            helper.assertTrue(link.getPackager() == null, "stock link incorrectly recognized the gas unpackager");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void stockLinkStillRecognizesGasPackager(GameTestHelper helper) {
        helper.setBlock(MACHINE, CCBBlocks.GAS_PACKAGER_BLOCK.getDefaultState());
        helper.setBlock(LINK, AllBlocks.STOCK_LINK.getDefaultState().setValue(PackagerLinkBlock.FACE, AttachFace.FLOOR));
        helper.runAfterDelay(2, () -> {
            PackagerLinkBlockEntity link = helper.getBlockEntity(LINK);
            helper.assertTrue(link.getPackager() != null, "stock link lost its connection to a real gas packager");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void unpackagerKeepsItsPackageOutputInventory(GameTestHelper helper) {
        helper.setBlock(MACHINE, CCBBlocks.GAS_UNPACKAGER_BLOCK.getDefaultState());
        GasUnpackagerBlockEntity unpackager = helper.getBlockEntity(MACHINE);
        IItemHandler handler = helper.getLevel().getCapability(ItemHandler.BLOCK, helper.absolutePos(MACHINE), null);
        helper.assertTrue(handler != null, "gas unpackager lost its item capability");
        if (handler == null) {
            throw new NullPointerException("Required handler is missing.");
        }

        helper.assertTrue(handler.getSlots() == 1, "gas unpackager lost its item capability");

        helper.assertTrue(unpackager.getAvailableItems().isEmpty(), "item capability was accidentally advertised as stock");
        ItemStack packageItem = BalloonFactory.create(new GasStack(CCBGases.NATURAL_AIR.get(), 1000));
        unpackager.heldBox = packageItem.copy();
        ItemStack extracted = handler.extractItem(0, 1, false);
        helper.assertTrue(ItemStack.isSameItemSameComponents(extracted, packageItem), "gas unpackager could not output its held package");
        helper.assertTrue(unpackager.heldBox.isEmpty(), "package output failed to empty the held slot");
        helper.succeed();
    }
}
