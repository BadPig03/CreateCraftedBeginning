package net.ty.createcraftedbeginning.gametests.compat;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
public final class FractionationTowerAssemblyGameTests {
    private static final int MIN_TOWER_HEIGHT = 3;
    private static final int MAX_TOWER_HEIGHT = 9;
    private static final BlockPos START = new BlockPos(3, 1, 3);
    private static final List<BlockPos> DESTINATIONS = List.of(new BlockPos(3, 2, 12), new BlockPos(12, 1, 12), new BlockPos(12, 2, 3), START);

    @GameTestGenerator
    public static Collection<TestFunction> towerAssembly() {
        if (!CCBCompatMods.SIMULATED.isLoaded() || !CCBCompatMods.SABLE.isLoaded()) {
            return List.of();
        }

        List<TestFunction> tests = new ArrayList<>();
        for (int height = MIN_TOWER_HEIGHT; height <= MAX_TOWER_HEIGHT; height++) {
            int towerHeight = height;
            tests.add(new TestFunction("physical_towers", "physical_towers.height_" + height, CCBAPI.MOD_ID + ":gametest/empty_20x12x20", 100, 0, true, helper -> run(helper, towerHeight)));
        }
        return tests;
    }

    private static void run(GameTestHelper helper, int height) {
        BlockPos center = helper.absolutePos(START);
        BlockPos neighbor = center.offset(3, 0, 0);
        assemble(helper, center, height);
        assemble(helper, neighbor, height);
        helper.runAfterDelay(3, () -> {
            ServerLevel level = helper.getLevel();
            Set<BlockPos> parts = positions(center, height);
            for (BlockPos part : parts) {
                helper.assertValueEqual(MultiblockAssemblyGameTests.gather(level, part), parts, "whole tower gathered from " + part.subtract(center));
            }
            seedLayerContents(helper, center, height);
            List<CompoundTag> contents = layerContents(level, center, height);
            moveAndVerify(helper, center, neighbor, height, contents, 0);
        });
    }

    private static void assemble(GameTestHelper helper, BlockPos center, int height) {
        ServerLevel level = helper.getLevel();
        BlockState tankState = CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState();
        for (BlockPos part : positions(center, height)) {
            level.setBlockAndUpdate(part, tankState);
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(center), Direction.UP, center, false);
        helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Tower assembly failed");
    }

    private static void seedLayerContents(GameTestHelper helper, BlockPos center, int height) {
        ServerLevel level = helper.getLevel();
        IItemHandler items = level.getCapability(ItemHandler.BLOCK, center, Direction.NORTH);
        if (items == null) {
            throw new NullPointerException("Expected tower item storage at " + center + '.');
        }

        IFluidHandler fluids = level.getCapability(FluidHandler.BLOCK, center, Direction.NORTH);
        if (fluids == null) {
            throw new NullPointerException("Expected tower fluid storage at " + center + '.');
        }

        GasHandler gases = level.getCapability(GasCapabilities.BLOCK, center, Direction.NORTH);
        if (gases == null) {
            throw new NullPointerException("Expected tower gas storage at " + center + '.');
        }

        Provider registries = level.registryAccess();
        BlockEntity controller = level.getBlockEntity(center);
        if (controller == null) {
            throw new NullPointerException("Expected tower controller at " + center + '.');
        }

        for (int layer = 0; layer < height; layer++) {
            helper.assertTrue(items.insertItem(0, new ItemStack(Items.DIAMOND, 2), false).isEmpty(), "Could not seed tower items");
            helper.assertValueEqual(fluids.fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE), 100, "seeded tower fluid");
            helper.assertValueEqual(gases.fill(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), GasAction.EXECUTE), 1000L, "seeded tower gas");
            if (layer == 0) {
                continue;
            }

            BlockPos targetPos = center.above(layer);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target == null) {
                throw new NullPointerException("Expected tower layer controller at " + targetPos + '.');
            }

            CompoundTag saved = target.saveWithFullMetadata(registries);
            saved.put("LayerInventory", controller.saveWithFullMetadata(registries).getCompound("LayerInventory").copy());
            target.loadWithComponents(saved, registries);
        }
        CompoundTag saved = controller.saveWithFullMetadata(registries);
        CompoundTag crafting = new CompoundTag();
        crafting.putString("Recipe", "createcraftedbeginning:test/assembly_progress");
        crafting.putInt("Progress", 17);
        crafting.putInt("Duration", 100);
        crafting.putBoolean("Paused", true);
        crafting.putString("PauseReason", "STRUCTURE");
        saved.put("Crafting", crafting);
        controller.loadWithComponents(saved, registries);
    }

    private static void moveAndVerify(GameTestHelper helper, BlockPos center, BlockPos neighbor, int height, List<CompoundTag> contents, int step) {
        ServerLevel level = helper.getLevel();
        BlockPos destination = helper.absolutePos(DESTINATIONS.get(step));
        Set<BlockPos> parts = positions(center, height);
        Provider registries = level.registryAccess();
        BlockEntity controller = level.getBlockEntity(center);
        if (controller == null) {
            throw new NullPointerException("Expected tower controller before movement at " + center + '.');
        }

        CompoundTag crafting = controller.saveWithFullMetadata(registries).getCompound("Crafting").copy();
        List<BlockPos> ordered = new ArrayList<>(parts);
        ordered.remove(center);
        ordered.add(center);
        MultiblockAssemblyGameTests.move(level, center, destination, Rotation.values()[step], ordered);
        for (BlockPos part : parts) {
            helper.assertTrue(level.getBlockState(part).isAir(), "Tower left a block at its old location");
        }
        BlockEntity movedController = level.getBlockEntity(destination);
        if (movedController == null) {
            throw new NullPointerException("Expected tower controller after movement at " + destination + '.');
        }

        helper.assertValueEqual(movedController.saveWithFullMetadata(registries).getCompound("Crafting"), crafting, "crafting state during movement");
        assertTower(helper, destination, height, contents);
        for (BlockPos part : positions(destination, height)) {
            BlockEntity entity = level.getBlockEntity(part);
            if (entity == null) {
                throw new NullPointerException("Expected tower block entity at " + part + '.');
            }

            CompoundTag saved = entity.saveWithFullMetadata(registries);
            level.removeBlockEntity(part);
            BlockEntity restored = BlockEntity.loadStatic(part, entity.getBlockState(), saved, registries);
            if (restored == null) {
                throw new NullPointerException("Expected reloaded tower block entity at " + part + '.');
            }

            level.setBlockEntity(restored);
        }
        helper.runAfterDelay(5, () -> {
            assertTower(helper, destination, height, contents);
            helper.assertValueEqual(MultiblockAssemblyGameTests.gather(level, neighbor), positions(neighbor, height), "adjacent tower remains intact");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, AABB.encapsulatingFullBlocks(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(19, 11, 19)))).isEmpty(), "Movement dropped tower items or its panel");
            if (step + 1 < DESTINATIONS.size()) {
                moveAndVerify(helper, destination, neighbor, height, contents, step + 1);
                return;
            }

            BlockPos broken = destination.offset(-1, height - 1, -1);
            level.destroyBlock(broken, true);
            for (BlockPos part : positions(destination, height)) {
                if (part.equals(broken)) {
                    continue;
                }

                helper.assertTrue(level.getBlockState(part).is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()), "Normal removal did not dismantle the relocated tower");
            }
            int panels = level.getEntitiesOfClass(ItemEntity.class, new AABB(destination).inflate(4, height + 1, 4)).stream().filter(item -> item.getItem().is(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get())).mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertValueEqual(panels, 1, "returned instrument panels");
            helper.assertValueEqual(MultiblockAssemblyGameTests.gather(level, neighbor), positions(neighbor, height), "adjacent tower survives disassembly");
            helper.succeed();
        });
    }

    private static void assertTower(GameTestHelper helper, BlockPos center, int height, List<CompoundTag> contents) {
        ServerLevel level = helper.getLevel();
        Provider registries = level.registryAccess();
        Set<BlockPos> parts = positions(center, height);
        long origin = center.offset(-1, 0, -1).asLong();
        helper.assertValueEqual(MultiblockAssemblyGameTests.gather(level, center), parts, "tower connectivity after relocation");
        helper.assertTrue(layerContents(level, center, height).equals(contents), "A layer's stored contents or capacity changed during movement");
        for (int layer = 0; layer < height; layer++) {
            BlockPos controller = center.above(layer);
            IItemHandler items = level.getCapability(ItemHandler.BLOCK, controller, Direction.NORTH);
            if (items == null) {
                throw new NullPointerException("Expected tower layer item storage at " + controller + '.');
            }

            IFluidHandler fluids = level.getCapability(FluidHandler.BLOCK, controller, Direction.NORTH);
            if (fluids == null) {
                throw new NullPointerException("Expected tower layer fluid storage at " + controller + '.');
            }

            GasHandler gases = level.getCapability(GasCapabilities.BLOCK, controller, Direction.NORTH);
            if (gases == null) {
                throw new NullPointerException("Expected tower layer gas storage at " + controller + '.');
            }

            for (BlockPos part : BlockPos.betweenClosed(controller.offset(-1, 0, -1), controller.offset(1, 0, 1))) {
                BlockEntity entity = level.getBlockEntity(part);
                if (entity == null) {
                    throw new NullPointerException("Expected tower block entity at " + part + '.');
                }

                CompoundTag saved = entity.saveWithFullMetadata(registries);
                helper.assertValueEqual(saved.getLong("TowerOrigin"), origin, "transformed tower origin");
                helper.assertValueEqual(saved.getInt("TowerHeight"), height, "tower height");
                helper.assertTrue(level.getCapability(ItemHandler.BLOCK, part, Direction.NORTH) == items, "Layer no longer shares its item storage");
                helper.assertTrue(level.getCapability(FluidHandler.BLOCK, part, Direction.NORTH) == fluids, "Layer no longer shares its fluid storage");
                helper.assertTrue(level.getCapability(GasCapabilities.BLOCK, part, Direction.NORTH) == gases, "Layer no longer shares its gas storage");
            }
            helper.assertTrue(items.getStackInSlot(0).is(Items.DIAMOND), "Layer contents are inaccessible");
            helper.assertValueEqual(items.extractItem(0, 1, true).getCount(), 1, "moved layer extraction");
            boolean inputLayer = layer == 0 || layer == height - 1;
            helper.assertValueEqual(items.insertItem(0, new ItemStack(Items.DIAMOND), true).isEmpty(), inputLayer, "layer input permissions");
        }
    }

    private static List<CompoundTag> layerContents(ServerLevel level, BlockPos center, int height) {
        List<CompoundTag> contents = new ArrayList<>();
        Provider registries = level.registryAccess();
        for (int layer = 0; layer < height; layer++) {
            BlockPos controller = center.above(layer);
            BlockEntity entity = level.getBlockEntity(controller);
            if (entity == null) {
                throw new NullPointerException("Expected tower layer controller at " + controller + '.');
            }

            CompoundTag inventory = entity.saveWithFullMetadata(registries).getCompound("LayerInventory").copy();

            for (Tag tank : inventory.getList("LayerFluidTanks", Tag.TAG_COMPOUND)) {
                ((CompoundTag) tank).remove("Level");
            }
            contents.add(inventory);
        }
        return contents;
    }

    private static Set<BlockPos> positions(BlockPos center, int height) {
        Set<BlockPos> parts = new HashSet<>();
        for (BlockPos part : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, height - 1, 1))) {
            parts.add(part.immutable());
        }
        return parts;
    }
}
