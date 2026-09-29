package net.ty.createcraftedbeginning.gametests.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
public final class MultiblockAssemblyGameTests {
    private static final BlockPos START = new BlockPos(3, 3, 3);
    private static final List<BlockPos> DESTINATIONS = List.of(new BlockPos(3, 3, 12), new BlockPos(12, 3, 12), new BlockPos(12, 3, 3), START);
    private static final String CONTRAPTION = "dev.simulated_team.simulated.util.assembly.SimAssemblyContraption";
    private static final String MOVER = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper";

    @GameTestGenerator
    public static Collection<TestFunction> physicalAssembly() {
        if (!CCBCompatMods.SIMULATED.isLoaded() || !CCBCompatMods.SABLE.isLoaded()) {
            return List.of();
        }

        List<TestFunction> tests = new ArrayList<>();
        tests.add(test("reactor", CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.getDefaultState()));
        tests.add(test("press", CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.getDefaultState()));
        BlockState turbineState = CCBBlocks.TESLA_TURBINE_BLOCK.getDefaultState();
        for (Axis axis : Axis.values()) {
            tests.add(test("turbine_" + axis.getName(), turbineState.setValue(TeslaTurbineBlock.AXIS, axis).setValue(TeslaTurbineBlock.ROTOR, 3)));
        }
        return tests;
    }

    @SuppressWarnings("unchecked")
    static Set<BlockPos> gather(ServerLevel level, BlockPos start) {
        try {
            Class<?> type = Class.forName(CONTRAPTION);
            Object contraption = type.getConstructor(BlockPos.class, boolean.class).newInstance(null, false);
            type.getMethod("searchMovedStructure", Level.class, BlockPos.class).invoke(contraption, level, start);
            Collection<BlockPos> blocks = (Collection<BlockPos>) type.getMethod("getBlocks").invoke(contraption);
            if (blocks == null) {
                throw new NullPointerException("Expected physical assembler blocks gathered from " + start + '.');
            }

            return new HashSet<>(blocks);
        }
        catch (ReflectiveOperationException exception) {
            throw reflectionFailure("gather machine blocks", start, exception);
        }
    }

    static void move(ServerLevel level, BlockPos from, BlockPos to, Rotation rotation, List<BlockPos> parts) {
        try {
            Class<?> transformType = Class.forName(MOVER + "$AssemblyTransform");
            Object transform = transformType.getConstructor(BlockPos.class, BlockPos.class, int.class, Rotation.class, ServerLevel.class).newInstance(from, to, -rotation.ordinal(), rotation, level);
            Class.forName(MOVER).getMethod("moveBlocks", ServerLevel.class, transformType, Iterable.class).invoke(null, level, transform, parts);
        }
        catch (ReflectiveOperationException exception) {
            throw reflectionFailure("move machine blocks", from, exception);
        }
    }

    private static TestFunction test(String name, BlockState state) {
        return new TestFunction("physical_multiblocks", "physical_multiblocks." + name, CCBAPI.MOD_ID + ":gametest/empty_20x12x20", 100, 0, true, helper -> run(helper, state));
    }

    private static void run(GameTestHelper helper, BlockState state) {
        BlockPos neighbor = state.hasProperty(TeslaTurbineBlock.AXIS) && state.getValue(TeslaTurbineBlock.AXIS) == Axis.X ? START.offset(0, 0, 3) : START.offset(3, 0, 0);
        helper.setBlock(START, state);
        helper.setBlock(neighbor, state);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            BlockPos core = helper.absolutePos(START);
            Set<BlockPos> expected = positions(core, state);
            for (BlockPos part : expected) {
                helper.assertValueEqual(gather(level, part), expected, "machine gathered from " + part.subtract(core));
            }
            seedContents(helper, core);
            assertContents(helper, core);
            moveAndVerify(helper, core, expected, 0, helper.absolutePos(neighbor));
        });
    }

    private static void moveAndVerify(GameTestHelper helper, BlockPos core, Set<BlockPos> parts, int step, BlockPos neighbor) {
        ServerLevel level = helper.getLevel();
        Provider registries = level.registryAccess();
        BlockState coreState = level.getBlockState(core);
        BlockState neighborState = level.getBlockState(neighbor);
        BlockPos destination = helper.absolutePos(DESTINATIONS.get(step));
        Rotation rotation = Rotation.values()[step];

        List<BlockPos> ordered = new ArrayList<>(parts);
        ordered.remove(core);
        ordered.add(core);
        move(level, core, destination, rotation, ordered);
        for (BlockPos old : parts) {
            helper.assertTrue(level.getBlockState(old).isAir(), "Old machine part remained at " + old);
        }
        Set<BlockPos> moved = new HashSet<>();
        for (BlockPos part : parts) {
            moved.add(destination.offset(part.subtract(core).rotate(rotation)));
        }
        helper.assertValueEqual(level.getBlockState(destination), coreState.rotate(rotation), "moved core state");
        assertContents(helper, destination);

        for (BlockPos part : moved) {
            BlockEntity entity = level.getBlockEntity(part);
            if (entity == null) {
                continue;
            }

            CompoundTag saved = entity.saveWithFullMetadata(registries);
            BlockState state = level.getBlockState(part);
            level.removeBlockEntity(part);
            BlockEntity restored = BlockEntity.loadStatic(part, state, saved, registries);
            if (restored == null) {
                throw new NullPointerException("Expected a reloaded machine block entity at " + part + '.');
            }

            level.setBlockEntity(restored);
        }
        helper.runAfterDelay(5, () -> {
            helper.assertValueEqual(gather(level, destination), moved, "complete structure after rotation and reload");
            helper.assertValueEqual(level.getBlockState(neighbor), neighborState, "neighboring machine core");
            helper.assertValueEqual(gather(level, neighbor), positions(neighbor, neighborState), "touching machine remains intact");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, AABB.encapsulatingFullBlocks(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(19, 11, 19)))).isEmpty(), "Assembly dropped or duplicated inventory/rotors");
            assertContents(helper, destination);
            if (step + 1 < DESTINATIONS.size()) {
                moveAndVerify(helper, destination, moved, step + 1, neighbor);
                return;
            }

            BlockPos brokenPart = moved.stream().filter(pos -> !pos.equals(destination)).findFirst().orElseThrow();
            level.destroyBlock(brokenPart, true);
            helper.assertTrue(level.getBlockState(destination).isAir(), "Normal breaking no longer destroys the machine core");
            for (BlockPos part : moved) {
                helper.assertTrue(level.getBlockState(part).isAir(), "Machine structure remained after destruction at " + part);
            }
            if (coreState.getBlock() instanceof TeslaTurbineBlock) {
                int droppedRotors = level.getEntitiesOfClass(ItemEntity.class, new AABB(destination).inflate(2)).stream().filter(item -> item.getItem().is(CCBItems.TESLA_TURBINE_ROTOR.get())).mapToInt(item -> item.getItem().getCount()).sum();
                helper.assertValueEqual(droppedRotors, coreState.getValue(TeslaTurbineBlock.ROTOR), "rotors returned once after destruction");
            }
            helper.assertValueEqual(gather(level, neighbor), positions(neighbor, neighborState), "touching machine survives normal destruction");
            helper.succeed();
        });
    }

    private static Set<BlockPos> positions(BlockPos core, BlockState state) {
        Set<BlockPos> positions = new HashSet<>();
        Axis turbineAxis = state.getBlock() instanceof TeslaTurbineBlock ? state.getValue(TeslaTurbineBlock.AXIS) : null;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (turbineAxis != null && turbineAxis.choose(x, y, z) != 0) {
                        continue;
                    }

                    positions.add(core.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    private static void seedContents(GameTestHelper helper, BlockPos core) {
        ServerLevel level = helper.getLevel();
        BlockEntity entity = level.getBlockEntity(core);
        if (entity == null) {
            throw new NullPointerException("Expected a machine core block entity at " + core + '.');
        }

        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1000);
        if (entity instanceof AirtightReactorKettleBlockEntity kettle) {
            kettle.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
            kettle.getOutputInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 5));
            kettle.getInputFluidTank().getCapability().fill(new FluidStack(Fluids.WATER, 750), FluidAction.EXECUTE);
            kettle.getInputGasTank().getCapability().fill(gas, GasAction.EXECUTE);
            return;
        }

        if (!(entity instanceof AirtightForgingPressBlockEntity press)) {
            return;
        }

        press.getInputInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
        press.getOutputInventory().setStackInSlot(0, new ItemStack(Items.GOLD_INGOT, 5));
        press.getPressHeadInventory().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        press.getFluidCapability().fill(new FluidStack(Fluids.WATER, 750), FluidAction.EXECUTE);
        press.getGasCapability().fill(gas, GasAction.EXECUTE);
    }

    private static void assertContents(GameTestHelper helper, BlockPos core) {
        ServerLevel level = helper.getLevel();
        BlockEntity entity = level.getBlockEntity(core);
        if (entity == null) {
            throw new NullPointerException("Expected a machine core block entity at " + core + '.');
        }

        if (entity instanceof AirtightReactorKettleBlockEntity kettle) {
            helper.assertValueEqual(kettle.getInputInventory().getStackInSlot(0).getCount(), 7, "reactor input items");
            helper.assertValueEqual(kettle.getOutputInventory().getStackInSlot(0).getCount(), 5, "reactor output items");
            helper.assertValueEqual(kettle.getInputFluidTank().getCapability().getFluidInTank(0).getAmount(), 750, "reactor fluid");
            helper.assertValueEqual(kettle.getInputGasTank().getCapability().getGasInTank(0).getAmount(), 1000L, "reactor gas");
            return;
        }

        if (entity instanceof AirtightForgingPressBlockEntity press) {
            helper.assertValueEqual(press.getInputInventory().getStackInSlot(0).getCount(), 7, "press input items");
            helper.assertValueEqual(press.getOutputInventory().getStackInSlot(0).getCount(), 5, "press output items");
            helper.assertTrue(press.getPressHeadInventory().getStackInSlot(0).is(Items.DIAMOND), "Press head was lost");
            helper.assertValueEqual(press.getFluidCapability().getFluidInTank(0).getAmount(), 750, "press fluid");
            helper.assertValueEqual(press.getGasCapability().getGasInTank(0).getAmount(), 1000L, "press gas");
            return;
        }

        BlockState state = level.getBlockState(core);
        helper.assertTrue(state.getBlock() instanceof TeslaTurbineBlock, "Machine core was lost");
        helper.assertValueEqual(state.getValue(TeslaTurbineBlock.ROTOR), 3, "turbine rotors");
    }

    private static RuntimeException reflectionFailure(String operation, BlockPos pos, ReflectiveOperationException exception) {
        Throwable cause = exception instanceof InvocationTargetException invocation ? invocation.getCause() : exception;
        return new IllegalStateException("Failed to " + operation + " at " + pos + '.', cause);
    }
}
