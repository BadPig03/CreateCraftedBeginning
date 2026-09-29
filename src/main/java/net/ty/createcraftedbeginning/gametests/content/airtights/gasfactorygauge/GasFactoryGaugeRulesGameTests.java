package net.ty.createcraftedbeginning.gametests.content.airtights.gasfactorygauge;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConfigurationPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeConnections;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeConnections.Check;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasFactoryGaugeRulesGameTests {
    private GasFactoryGaugeRulesGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void mixedConnectionsKeepDirectionLimitsAndFallback(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        FactoryPanelBehaviour gas = fixture.gas;
        FactoryPanelBehaviour item = fixture.item;
        fixture.assertIssue(gas, item, null);
        fixture.assertIssue(item, gas, null);
        helper.assertTrue(GasFactoryGaugeConnections.check(gas, gas) == null, "Same-kind connection did not defer to Create");
        GasFactoryGaugeConnections.check(null, item);
        gas.targetedBy.put(item.getPanelPosition(), new FactoryPanelConnection(item.getPanelPosition(), 1));
        fixture.assertIssue(gas, item, "factory_panel.already_connected");
        fixture.assertIssue(item, gas, null);
        gas.targetedBy.clear();
        for (int i = 0; i < 9; i++) {
            FactoryPanelPosition key = new FactoryPanelPosition(gas.getPos().offset(i, 10, 0), PanelSlot.BOTTOM_LEFT);
            gas.targetedBy.put(key, new FactoryPanelConnection(key, 1));
        }
        fixture.assertIssue(gas, item, "factory_panel.cannot_add_more_inputs");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void mixedConnectionsRejectOrientationSurfaceRestockerAndEmptyFilter(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        FactoryPanelBehaviour gas = fixture.gas;
        FactoryPanelBehaviour item = fixture.item;
        BlockPos local = new BlockPos(3, 1, 1);
        BlockState original = item.blockEntity.getBlockState();
        helper.setBlock(local, original.setValue(FactoryPanelBlock.FACING, Direction.SOUTH));
        fixture.assertIssue(gas, item, "factory_panel.same_orientation");
        helper.setBlock(local, original);
        item.panelBE().restocker = true;
        fixture.assertIssue(gas, item, "factory_panel.input_in_restock_mode");
        item.panelBE().restocker = false;
        item.setFilter(ItemStack.EMPTY);
        fixture.assertIssue(gas, item, "factory_panel.no_item");
        FactoryPanelBehaviour raised = fixture.panel(new BlockPos(3, 3, 1), false);
        fixture.assertIssue(gas, raised, "factory_panel.same_surface");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void realConfigurationPacketClampsGasInputsOnly(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        FactoryPanelBehaviour target = fixture.item;
        FactoryPanelPosition source = fixture.gas.getPanelPosition();
        target.targetedBy.put(source, new FactoryPanelConnection(source, 1));
        Map<FactoryPanelPosition, Integer> inputs = new HashMap<>();
        inputs.put(source, Integer.MAX_VALUE);
        new Packet(target.getPanelPosition(), inputs, 7).apply(target.panelBE());
        helper.assertValueEqual(target.targetedBy.get(source).amount, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT, "Gas input upper bound");
        helper.assertValueEqual(target.recipeOutput, 7, "Ordinary panel output changed");
        helper.assertTrue(!target.activeCraftingArrangement.isEmpty(), "Ordinary panel crafting was cleared");
        inputs.put(source, Integer.MIN_VALUE);
        new Packet(target.getPanelPosition(), inputs, 7).apply(target.panelBE());
        helper.assertValueEqual(target.targetedBy.get(source).amount, 0, "Gas input lower bound");
        FactoryPanelPosition itemSource = target.getPanelPosition();
        fixture.gas.targetedBy.put(itemSource, new FactoryPanelConnection(itemSource, 1));
        inputs.clear();
        inputs.put(itemSource, 64);
        new Packet(source, inputs, 1).apply(fixture.gas.panelBE());
        helper.assertValueEqual(fixture.gas.targetedBy.get(itemSource).amount, 64, "Ordinary input amount changed");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void realConfigurationPacketClampsOutputAndClearsGasCrafting(GameTestHelper helper) {
        Fixture fixture = new Fixture(helper);
        FactoryPanelBehaviour gas = fixture.gas;
        new Packet(gas.getPanelPosition(), new HashMap<>(), Integer.MAX_VALUE).apply(gas.panelBE());
        helper.assertValueEqual(gas.recipeOutput, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT, "Gas output upper bound");
        helper.assertTrue(gas.activeCraftingArrangement.isEmpty(), "Packet left item crafting on gas panel");
        new Packet(gas.getPanelPosition(), new HashMap<>(), Integer.MIN_VALUE).apply(gas.panelBE());
        helper.assertValueEqual(gas.recipeOutput, 1, "Gas output lower bound");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void mixedConnectionDistanceExcludesSixteenBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Fixture fixture = new Fixture(helper);
        List<BlockPos> positions = List.of(new BlockPos(1, 100, 1), new BlockPos(16, 100, 1), new BlockPos(17, 100, 1));
        for (BlockPos pos : positions) {
            helper.assertTrue(level.getBlockState(helper.absolutePos(pos)).isAir() && level.getBlockState(helper.absolutePos(pos.below())).isAir(), "Distance fixture positions must be empty");
        }
        try {
            FactoryPanelBehaviour gas = fixture.panel(positions.get(0), true);
            FactoryPanelBehaviour near = fixture.panel(positions.get(1), false);
            FactoryPanelBehaviour far = fixture.panel(positions.get(2), false);
            fixture.assertIssue(gas, near, null);
            fixture.assertIssue(gas, far, "factory_panel.too_far_apart");
        }
        finally {
            for (BlockPos pos : positions) {
                helper.setBlock(pos, Blocks.AIR);
                helper.setBlock(pos.below(), Blocks.AIR);
            }
        }
        helper.succeed();
    }

    private static final class Fixture {
        private final GameTestHelper helper;
        private final FactoryPanelBehaviour gas;
        private final FactoryPanelBehaviour item;

        private Fixture(GameTestHelper helper) {
            this.helper = helper;
            gas = panel(new BlockPos(1, 1, 1), true);
            item = panel(new BlockPos(3, 1, 1), false);
        }

        private FactoryPanelBehaviour panel(BlockPos pos, boolean gas) {
            helper.setBlock(pos.below(), Blocks.STONE);
            BlockState state = (gas ? CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.get() : AllBlocks.FACTORY_GAUGE.get()).defaultBlockState().setValue(FactoryPanelBlock.FACE, AttachFace.FLOOR).setValue(FactoryPanelBlock.FACING, Direction.NORTH);
            helper.setBlock(pos, state);
            FactoryPanelBlockEntity block = helper.getBlockEntity(pos);
            FactoryPanelBehaviour panel = block.panels.get(PanelSlot.BOTTOM_LEFT);
            panel.enable();
            panel.setFilter(gas ? VirtualGasItems.createVirtualItem(new GasStack(CCBGases.NATURAL_AIR.get(), 1)) : new ItemStack(Items.IRON_INGOT));
            return panel;
        }

        private void assertIssue(FactoryPanelBehaviour from, FactoryPanelBehaviour to, @Nullable String expected) {
            Check check = GasFactoryGaugeConnections.check(from, to);
            helper.assertTrue(check != null && Objects.equals(check.issue(), expected), "Unexpected connection result: " + check + ", expected " + expected);
        }
    }

    private static final class Packet extends FactoryPanelConfigurationPacket {
        private Packet(FactoryPanelPosition position, Map<FactoryPanelPosition, Integer> inputs, int output) {
            super(position, "test", inputs, List.of(new ItemStack(Items.IRON_INGOT)), output, 20, null, false, false, false);
        }

        private void apply(FactoryPanelBlockEntity panel) {
            applySettings(null, panel);
        }
    }
}
