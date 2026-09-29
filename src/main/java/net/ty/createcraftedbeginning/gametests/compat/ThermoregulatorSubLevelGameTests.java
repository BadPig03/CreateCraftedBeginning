package net.ty.createcraftedbeginning.gametests.compat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBasinCooling;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.Fixture;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.joml.Quaterniond;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.assemble;
import static net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.clear;
import static net.ty.createcraftedbeginning.gametests.compat.SubLevelGameTestFixtures.move;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ThermoregulatorSubLevelGameTests {
    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void localBasinCooling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos basin = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos source = basin.below();
        level.setBlockAndUpdate(basin, AllBlocks.BASIN.getDefaultState());
        assertCooling(helper, basin, false);
        setCooler(level, source, true);
        assertCooling(helper, basin, true);
        setCooler(level, source, false);
        assertCooling(helper, basin, false);
        level.setBlockAndUpdate(source, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
        assertCooling(helper, basin, false);
        level.setBlockAndUpdate(source, Blocks.AIR.defaultBlockState());
        assertCooling(helper, basin, false);
        helper.succeed();
    }

    @GameTestGenerator
    public static Collection<TestFunction> temperatureAcrossSpaces() {
        if (!CCBCompatMods.SIMULATED.isLoaded() || !CCBCompatMods.SABLE.isLoaded()) {
            return List.of();
        }

        String template = CCBAPI.MOD_ID + ":gametest/empty_20x12x20";
        return List.of(new TestFunction("thermal_reactor", "thermal_reactor.sources", template, 100, 0, true, helper -> machineTemperature(helper, false)), new TestFunction("thermal_tower", "thermal_tower.sources", template, 100, 0, true, helper -> machineTemperature(helper, true)), new TestFunction("thermal_basin", "thermal_basin.cooling", template, 100, 0, true, ThermoregulatorSubLevelGameTests::basinCooling));
    }

    private static void machineTemperature(GameTestHelper helper, boolean tower) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(8, 4, 8));
        if (tower) {
            BlockState tank = CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState();
            for (BlockPos pos : BlockPos.betweenClosed(core.offset(-1, 0, -1), core.offset(1, 2, 1))) {
                level.setBlockAndUpdate(pos, tank);
            }
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(core), Direction.UP, core, false);
            helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Temperature test tower assembly failed");
        }
        else {
            level.setBlockAndUpdate(core, CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState());
        }
        helper.runAfterDelay(3, () -> {
            List<Fixture> fixtures = new ArrayList<>();
            Set<BlockPos> worldSources = new HashSet<>();
            try {
                Fixture machine = assemble(level, core, MultiblockAssemblyGameTests.gather(level, core));
                fixtures.add(machine);
                int depth = tower ? 1 : 2;
                BlockPos source = core.below(depth);
                BlockPos localSource = machine.center().below(depth);
                BlockState heated = AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED);
                BlockState superheated = heated.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING);
                BlockState chilled = CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED);
                worldSources.add(source);
                worldSources.add(source.east());
                level.setBlockAndUpdate(source, superheated);
                level.setBlockAndUpdate(source.east(), chilled);
                assertTemperature(helper, machine.center(), 2, "mixed");
                level.setBlockAndUpdate(source.east(), Blocks.AIR.defaultBlockState());
                assertTemperature(helper, machine.center(), 3, "heat");
                level.setBlockAndUpdate(localSource, chilled);
                move(machine, Vec3.atCenterOf(core), new Quaterniond());
                assertTemperature(helper, machine.center(), -1, "local_priority");
                level.setBlockAndUpdate(localSource, Blocks.AIR.defaultBlockState());

                move(machine, Vec3.atCenterOf(core).add(5, 0, 0), new Quaterniond());
                assertTemperature(helper, machine.center(), 0, "move_away");
                move(machine, Vec3.atCenterOf(core), new Quaterniond().rotationY(Math.PI / 2));
                assertTemperature(helper, machine.center(), 3, "yaw");
                move(machine, Vec3.atCenterOf(core), new Quaterniond());

                Fixture burner = assemble(level, source, Set.of(source));
                fixtures.add(burner);
                assertTemperature(helper, machine.center(), 3, "separate_source");
                level.setBlockAndUpdate(burner.center(), heated.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
                move(burner, Vec3.atCenterOf(source), new Quaterniond());
                assertTemperature(helper, machine.center(), 0, "inactive_source");
                level.setBlockAndUpdate(burner.center(), chilled);
                move(burner, Vec3.atCenterOf(source), new Quaterniond());
                assertTemperature(helper, machine.center(), -1, "cold_source");
                move(burner, Vec3.atCenterOf(source).add(5, 0, 0), new Quaterniond());
                assertTemperature(helper, machine.center(), 0, "source_moved");

                move(machine, Vec3.atCenterOf(core).add(0.3, 0, 0.3), new Quaterniond().rotationY(Math.PI / 4));
                Set<BlockPos> projected = new HashSet<>();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Vec3 point = machine.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(localSource.offset(x, 0, z)));
                        projected.add(BlockPos.containing(point));
                    }
                }
                helper.assertTrue(projected.size() < 9, "Rotated sampling fixture must hit a source more than once");
                worldSources.addAll(projected);
                for (BlockPos pos : projected) {
                    level.setBlockAndUpdate(pos, heated);
                }
                assertTemperature(helper, machine.center(), projected.size(), "deduplicated");
                for (BlockPos pos : worldSources) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
                assertTemperature(helper, machine.center(), 0, "cleared");

                move(machine, Vec3.atCenterOf(core), new Quaterniond().rotationX(Math.PI / 2));
                BlockPos wallSource = BlockPos.containing(machine.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(localSource)));
                worldSources.add(wallSource);
                level.setBlockAndUpdate(wallSource, superheated);
                assertTemperature(helper, machine.center(), 3, "pitch");
                helper.succeed();
            }
            finally {
                for (Fixture fixture : fixtures) {
                    clear(level, fixture);
                }
                for (BlockPos pos : worldSources) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        });
    }

    private static void basinCooling(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos basinPos = helper.absolutePos(new BlockPos(8, 4, 8));
        BlockPos coolerPos = basinPos.below();
        level.setBlockAndUpdate(basinPos, AllBlocks.BASIN.getDefaultState());
        setCooler(level, coolerPos, true);
        List<Fixture> fixtures = new ArrayList<>();
        try {
            assertCooling(helper, basinPos, true);
            Fixture basin = assemble(level, basinPos, Set.of(basinPos));
            fixtures.add(basin);
            assertCooling(helper, basin.center(), true);
            move(basin, Vec3.atCenterOf(basinPos).add(4, 0, 0), new Quaterniond());
            assertCooling(helper, basin.center(), false);
            move(basin, Vec3.atCenterOf(basinPos), new Quaterniond().rotationY(Math.PI / 2));
            assertCooling(helper, basin.center(), true);

            Fixture cooler = assemble(level, coolerPos, Set.of(coolerPos));
            fixtures.add(cooler);
            assertCooling(helper, basin.center(), true);
            setCooler(level, cooler.center(), false);
            move(cooler, Vec3.atCenterOf(coolerPos), new Quaterniond());
            assertCooling(helper, basin.center(), false);
            setCooler(level, cooler.center(), true);
            move(cooler, Vec3.atCenterOf(coolerPos).add(4, 0, 0), new Quaterniond());
            assertCooling(helper, basin.center(), false);

            level.setBlockAndUpdate(basinPos, AllBlocks.BASIN.getDefaultState());
            move(cooler, Vec3.atCenterOf(coolerPos), new Quaterniond());
            assertCooling(helper, basinPos, true);
            level.setBlockAndUpdate(coolerPos, AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
            assertCooling(helper, basinPos, true);
            move(cooler, Vec3.atCenterOf(coolerPos).add(4, 0, 0), new Quaterniond());
            assertCooling(helper, basinPos, false);
            helper.succeed();
        }
        finally {
            for (Fixture fixture : fixtures) {
                clear(level, fixture);
            }
            level.setBlockAndUpdate(basinPos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(coolerPos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void assertTemperature(GameTestHelper helper, BlockPos core, float expected, String stage) {
        BlockEntity entity = helper.getLevel().getBlockEntity(core);
        if (entity == null) {
            throw new NullPointerException("Expected a thermal machine at " + core + '.');
        }

        if (entity instanceof AirtightReactorKettleBlockEntity kettle) {
            kettle.getCore().getStructureManager().tick();
            helper.assertValueEqual(kettle.getRecipeTemperature(), expected, "reactor temperature across spaces: " + stage);
            return;
        }

        AirtightFractionationTowerBlockEntity tower = (AirtightFractionationTowerBlockEntity) entity;
        tower.tick();
        helper.assertValueEqual(tower.getRecipeTemperature(), expected, "tower temperature across spaces: " + stage);
    }

    private static void assertCooling(GameTestHelper helper, BlockPos pos, boolean expected) {
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        if (entity == null) {
            throw new NullPointerException("Expected a basin at " + pos + '.');
        }

        helper.assertValueEqual(BreezeCoolerBasinCooling.hasChilledSource((BasinBlockEntity) entity), expected, "basin cooling across spaces");
    }

    private static void setCooler(ServerLevel level, BlockPos pos, boolean chilled) {
        FrostLevel frost = chilled ? FrostLevel.CHILLED : FrostLevel.RIMING;
        level.setBlockAndUpdate(pos, CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, frost));
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            throw new NullPointerException("Expected a cooler at " + pos + '.');
        }

        CompoundTag data = new CompoundTag();
        data.putString("StateType", chilled ? "NORMAL" : "NONE");
        CompoundTag state = new CompoundTag();
        state.putBoolean("isCreative", chilled);
        data.put("StateData", state);
        entity.loadWithComponents(data, level.registryAccess());
    }

}
