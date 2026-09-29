package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.item.SmartInventory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUnits;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.logistics.GasInventoryIdentifierProvider;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.config.CCBMachines.AirtightFractionationTower;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerFailurePacket;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerMode;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.gametests.recipe.RecipeIndexTestScope;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.FractionationTowerCraftPlanner;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightFractionationTowerGameTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private AirtightFractionationTowerGameTests() {
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void minimumTowerAssemblesAndDisassembles(GameTestHelper helper) {
        verifyAssemblyAndRemoval(helper, 3);
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void maximumTowerSurvivesSerializationAndDisassembles(GameTestHelper helper) {
        verifyAssemblyAndRemoval(helper, 9);
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 40)
    public static void maximumTowerAcrossFourChunksDropsEachLayerOnce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(ORIGIN);
        BlockPos origin = ORIGIN.offset(15 - (base.getX() & 15), 0, 15 - (base.getZ() & 15));
        BlockPos absoluteOrigin = helper.absolutePos(origin);
        BlockPos end = origin.offset(2, 8, 2);
        ChunkPos minChunk = new ChunkPos(absoluteOrigin);
        ChunkPos maxChunk = new ChunkPos(helper.absolutePos(end));
        helper.assertTrue(maxChunk.x == minChunk.x + 1 && maxChunk.z == minChunk.z + 1, "The tower fixture must span four chunks.");
        placeTank(helper, origin, 3, 9);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.assertTrue(usePanel(helper, player, end).consumesAction(), "A tower crossing chunk boundaries must assemble from its far corner.");
        LayerPorts[] layers = new LayerPorts[9];
        layers[0] = requireLayerPorts(helper, origin);
        layers[0].items().insertItem(0, new ItemStack(Items.DIAMOND, 12), false);
        layers[0].fluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        layers[0].gases().fill(new GasStack(CCBGases.MOIST_AIR.get(), 1000), GasAction.EXECUTE);
        for (int layer = 1; layer < layers.length; layer++) {
            layers[layer] = copyLayerInventory(helper, origin, 0, layer);
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin, end)) {
            LayerPorts expected = layers[pos.getY() - origin.getY()];
            LayerPorts actual = requireLayerPorts(helper, pos);
            helper.assertTrue(actual.items() == expected.items() && actual.fluids() == expected.fluids() && actual.gases() == expected.gases(), "Members across chunk boundaries must share only their own layer inventories.");
        }
        level.destroyBlock(absoluteOrigin, true);
        for (LayerPorts ports : layers) {
            helper.assertTrue(ports.items().getStackInSlot(0).isEmpty() && ports.fluids().getFluidInTank(0).isEmpty() && ports.gases().getGasInTank(0).isEmpty(), "Cross-chunk disassembly must empty every cached layer handler.");
            assertInsertionRejected(helper, ports);
        }
        for (BlockPos pos : BlockPos.betweenClosed(origin, end)) {
            if (pos.equals(origin)) {
                continue;
            }

            BlockPos absolutePos = helper.absolutePos(pos);
            helper.assertTrue(level.getBlockState(absolutePos).is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()), "Cross-chunk disassembly must restore all remaining tank blocks.");
            GasHandler gas = level.getCapability(GasCapabilities.BLOCK, absolutePos, Direction.NORTH);
            if (gas == null) {
                throw new NullPointerException("Expected a restored tank gas capability at " + absolutePos + '.');
            }

            helper.assertTrue(gas.getGasInTank(0).isEmpty(), "Restored tanks across chunk boundaries must not inherit tower gas.");
        }
        level.destroyBlock(helper.absolutePos(end), true);
        AABB bounds = new AABB(absoluteOrigin).expandTowards(3, 9, 3).inflate(1);
        AABB dropBounds = new AABB(absoluteOrigin);
        int diamonds = 0;
        int panels = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = item.getItem();
            if (stack.is(Items.DIAMOND)) {
                helper.assertTrue(dropBounds.contains(item.position()), "Every layer must drop its stored items at the original break position.");
                diamonds += stack.getCount();
            }
            if (!stack.is(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get())) {
                continue;
            }

            helper.assertTrue(dropBounds.contains(item.position()), "The instrument panel must share the stored item drop position.");
            panels += stack.getCount();
        }
        helper.assertValueEqual(diamonds, 108, "stored item drops from nine cross-chunk layers");
        helper.assertValueEqual(panels, 1, "instrument panel drops after repeated cross-chunk removal");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void rejectedOutputRollsBackMixedInputsAndEarlierProducts(GameTestHelper helper) {
        verifyOutputRollback(helper, false);
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void outputExceptionRollsBackMixedInputsAndEarlierProducts(GameTestHelper helper) {
        verifyOutputRollback(helper, true);
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void nonEmptyTankRejectsAssemblyWithoutConsumingPanel(GameTestHelper helper) {
        AirtightTankBlockEntity tank = placeTank(helper, 3, 3);
        tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 100)).requireAccepted();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.assertTrue(usePanel(helper, player, ORIGIN.offset(2, 2, 2)) == InteractionResult.FAIL, "Non-empty tank must reject tower assembly.");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Rejected assembly must preserve the instrument panel.");
        helper.assertTrue(tank.getGas(0).getAmount() == 100, "Rejected assembly must preserve the gas contents.");
        assertTankBlocks(helper, 3, 3);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void invalidDimensionsRejectAssembly(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        int[][] dimensions = {{1, 3}, {2, 3}, {3, 2}, {3, 10}};
        for (int[] dimension : dimensions) {
            int width = dimension[0];
            int height = dimension[1];
            placeTank(helper, width, height);
            helper.assertTrue(usePanel(helper, player, ORIGIN) == InteractionResult.FAIL, "Invalid tank dimensions must reject tower assembly.");
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Invalid dimensions must preserve the instrument panel.");
            assertTankBlocks(helper, width, height);
            for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(width - 1, height - 1, width - 1))) {
                helper.setBlock(pos, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void assemblyRejectsInvalidTargetsHolesAndPermissions(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.setBlock(ORIGIN, Blocks.STONE);
        helper.assertTrue(usePanel(helper, player, ORIGIN) == InteractionResult.FAIL, "The instrument panel must reject an unrelated block without assembly.");
        helper.setBlock(ORIGIN, CCBBlocks.HORIZONTAL_AIRTIGHT_TANK_BLOCK.getDefaultState());
        helper.assertTrue(usePanel(helper, player, ORIGIN) == InteractionResult.FAIL, "A horizontal tank must reject assembly.");
        placeTank(helper, 3, 3);
        helper.setBlock(ORIGIN.offset(1, 1, 1), Blocks.AIR);
        helper.assertTrue(usePanel(helper, player, ORIGIN) == InteractionResult.FAIL, "A hollow tank structure must reject assembly.");
        helper.setBlock(ORIGIN.offset(1, 1, 1), CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState());
        player.getAbilities().mayBuild = false;
        BlockPos absoluteOrigin = helper.absolutePos(ORIGIN);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteOrigin), Direction.NORTH, absoluteOrigin, false);
        helper.assertTrue(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.FAIL, "The instrument panel must reject interaction without build permission.");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Failed assembly must preserve the instrument panel.");
        player.getAbilities().mayBuild = true;
        helper.assertTrue(usePanel(helper, player, ORIGIN).consumesAction(), "A repaired tank structure must assemble.");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.assertTrue(usePanel(helper, player, ORIGIN) == InteractionResult.FAIL, "An assembled tower must reject a second panel.");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "An assembled tower must not consume another panel.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void failurePacketPreservesBoundsAndWarningReason(GameTestHelper helper) {
        BlockPos min = helper.absolutePos(ORIGIN);
        BlockPos max = min.offset(2, 8, 2);
        for (String reason : new String[]{"invalid_size", "gas_inside", "already_assembled", "invalid_structure"}) {
            ByteBuf buffer = Unpooled.buffer();
            try {
                AirtightFractionationTowerFailurePacket original = new AirtightFractionationTowerFailurePacket(min, max, reason);
                AirtightFractionationTowerFailurePacket.STREAM_CODEC.encode(buffer, original);
                AirtightFractionationTowerFailurePacket decoded = AirtightFractionationTowerFailurePacket.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.equals(original), "Assembly feedback must preserve the outline bounds and warning reason.");
                helper.assertTrue(!buffer.isReadable(), "Assembly feedback must decode the entire packet.");
            }
            finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void layerStorageIsSharedIsolatedAndPersistent(GameTestHelper helper) {
        assembleTower(helper);
        ServerLevel level = helper.getLevel();
        Provider provider = level.registryAccess();
        BlockPos center = ORIGIN.offset(1, 0, 1);
        LayerPorts bottom = requireLayerPorts(helper, center);
        LayerPorts above = requireLayerPorts(helper, center.above());
        AirtightFractionationTower config = CCBConfig.server().machines.airtightFractionationTower;
        helper.assertTrue(bottom.items().getSlots() == config.itemSlotsPerLayer.get(), "Item slot count must follow the tower configuration.");
        helper.assertTrue(bottom.fluids().getTanks() == config.fluidTanksPerLayer.get(), "Fluid tank count must follow the tower configuration.");
        helper.assertTrue(bottom.gases().getTanks() == config.gasTanksPerLayer.get(), "Gas tank count must follow the tower configuration.");
        helper.assertTrue(bottom.fluids().getTankCapacity(0) == config.fluidCapacityPerTank.get() * FluidType.BUCKET_VOLUME, "Fluid capacity must convert configured buckets to millibuckets.");
        helper.assertTrue(bottom.gases().getTankVolume(0) == config.gasVolumePerTank.get() * GasUnits.LITERS_PER_KILOLITER, "Gas volume must convert configured kiloliters to liters.");
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(2, 0, 2))) {
            LayerPorts member = requireLayerPorts(helper, pos);
            helper.assertTrue(member.items() == bottom.items() && member.fluids() == bottom.fluids() && member.gases() == bottom.gases(), "All nine members must expose the same layer handlers.");
            helper.assertTrue(member.identifier().equals(bottom.identifier()), "All nine members must identify the same gas inventory.");
            BlockEntity tower = level.getBlockEntity(helper.absolutePos(pos));
            if (tower == null) {
                throw new NullPointerException("Expected a tower member at " + pos + '.');
            }

            helper.assertTrue(tower.saveWithFullMetadata(provider).contains("LayerInventory") == pos.equals(center), "Only the center member may serialize layer contents.");
        }
        helper.assertTrue(above.items() != bottom.items() && above.fluids() != bottom.fluids() && above.gases() != bottom.gases() && !above.identifier().equals(bottom.identifier()), "Different layers must own independent storage and gas identities.");
        bottom.items().insertItem(0, new ItemStack(Items.DIAMOND, 12), true);
        bottom.fluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE);
        bottom.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), GasAction.SIMULATE);
        helper.assertTrue(bottom.items().getStackInSlot(0).isEmpty() && bottom.fluids().getFluidInTank(0).isEmpty() && bottom.gases().getGasInTank(0).isEmpty(), "Simulation must not change any layer storage.");
        bottom.items().insertItem(0, new ItemStack(Items.DIAMOND, 12), false);
        bottom.fluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
        bottom.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 1000), GasAction.EXECUTE);
        LayerPorts corner = requireLayerPorts(helper, ORIGIN.offset(2, 0, 2));
        helper.assertTrue(corner.items().extractItem(0, 2, false).getCount() == 2 && corner.fluids().drain(100, FluidAction.EXECUTE).getAmount() == 100 && corner.gases().drain(100, GasAction.EXECUTE).getAmount() == 100, "A different member must extract from the same inventories.");
        helper.assertTrue(above.items().getStackInSlot(0).isEmpty() && above.fluids().getFluidInTank(0).isEmpty() && above.gases().getGasInTank(0).isEmpty(), "Layer access must not affect the layer above.");
        BlockPos absoluteCenter = helper.absolutePos(center);
        BlockEntity controller = level.getBlockEntity(absoluteCenter);
        if (controller == null) {
            throw new NullPointerException("Expected a layer controller at " + absoluteCenter + '.');
        }

        CompoundTag saved = controller.saveWithFullMetadata(provider);
        controller.handleUpdateTag(saved, provider);
        LayerPorts synchronizedPorts = requireLayerPorts(helper, center);
        helper.assertTrue(synchronizedPorts.items() == bottom.items() && synchronizedPorts.fluids() == bottom.fluids() && synchronizedPorts.gases() == bottom.gases(), "Synchronizing unchanged storage dimensions must preserve all layer handler instances.");
        helper.assertTrue(bottom.items().getStackInSlot(0).getCount() == 10 && bottom.fluids().getFluidInTank(0).getAmount() == 900 && bottom.gases().getGasInTank(0).getAmount() == 900, "Reused handlers must retain synchronized contents.");
        BlockEntity receiver = controller.getType().create(absoluteCenter, controller.getBlockState());
        if (receiver == null) {
            throw new NullPointerException("Failed to create tower inventory packet receiver at " + absoluteCenter + '.');
        }

        receiver.handleUpdateTag(controller.getUpdateTag(provider), provider);
        CompoundTag synchronizedInventory = receiver.getUpdateTag(provider).getCompound("LayerInventory");
        CompoundTag savedInventory = saved.getCompound("LayerInventory");
        helper.assertTrue(synchronizedInventory.getCompound("Items").equals(savedInventory.getCompound("Items")) && synchronizedInventory.getList("LayerGasTanks", Tag.TAG_COMPOUND).equals(savedInventory.getList("LayerGasTanks", Tag.TAG_COMPOUND)), "Synchronization must restore item and gas contents.");
        helper.assertTrue(synchronizedInventory.getList("LayerFluidTanks", Tag.TAG_COMPOUND).getCompound(0).getCompound("TankContent").equals(savedInventory.getList("LayerFluidTanks", Tag.TAG_COMPOUND).getCompound(0).getCompound("TankContent")), "Synchronization must restore fluid contents independently of client interpolation.");
        receiver.handleUpdateTag(new CompoundTag(), provider);
        helper.assertTrue(!receiver.getUpdateTag(provider).contains("LayerInventory"), "Clearing the structure must also clear synchronized contents.");
        CompoundTag inventoryTag = saved.getCompound("LayerInventory");
        inventoryTag.putInt("ItemSlots", 13);
        inventoryTag.getCompound("Items").putInt("Size", 13);
        inventoryTag.putInt("FluidTanks", 3);
        inventoryTag.putInt("GasTanks", 2);
        inventoryTag.putInt("FluidCapacity", 17000);
        inventoryTag.putLong("GasVolume", 230000);
        BlockEntity restored = BlockEntity.loadStatic(absoluteCenter, controller.getBlockState(), saved, provider);
        if (restored == null) {
            throw new NullPointerException("Failed to reload a populated tower layer at " + absoluteCenter + '.');
        }

        level.setBlockEntity(restored);
        LayerPorts reloaded = requireLayerPorts(helper, ORIGIN);
        helper.assertTrue(reloaded.items().getStackInSlot(0).getCount() == 10 && reloaded.fluids().getFluidInTank(0).getAmount() == 900 && reloaded.gases().getGasInTank(0).getAmount() == 900, "Reloading the center must preserve each inventory and redirect member capabilities.");
        helper.assertTrue(reloaded.items().getSlots() == 13 && reloaded.fluids().getTanks() == 3 && reloaded.gases().getTanks() == 2, "Saved item and tank counts must take precedence over current configuration.");
        helper.assertTrue(reloaded.fluids().getTankCapacity(0) == 17000 && reloaded.gases().getTankVolume(0) == 230000, "Saved storage dimensions must take precedence over current configuration.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void onlyEndLayersAcceptExternalInputs(GameTestHelper helper) {
        placeTank(helper, 3, 9);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.assertTrue(usePanel(helper, player, ORIGIN).consumesAction(), "Expected a nine-layer tower to assemble.");
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(2, 8, 2))) {
            LayerPorts ports = requireLayerPorts(helper, pos);
            int layer = pos.getY() - ORIGIN.getY();
            if (layer != 0 && layer != 8) {
                assertInsertionRejected(helper, ports);
                continue;
            }

            helper.assertTrue(ports.items().insertItem(0, new ItemStack(Items.DIAMOND), true).isEmpty(), "End layers must accept simulated item insertion.");
            helper.assertValueEqual(ports.fluids().fill(new FluidStack(Fluids.WATER, 100), FluidAction.SIMULATE), 100, "simulated end-layer fluid insertion");
            helper.assertValueEqual(ports.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), GasAction.SIMULATE), 100L, "simulated end-layer gas insertion");
            helper.assertTrue(ports.items().insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty(), "End layers must accept item insertion through every member.");
            helper.assertValueEqual(ports.fluids().fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE), 100, "end-layer fluid insertion");
            helper.assertValueEqual(ports.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), GasAction.EXECUTE), 100L, "end-layer gas insertion");
        }
        LayerPorts middle = copyLayerInventory(helper, 0, 4);
        assertInsertionRejected(helper, middle);
        helper.assertTrue(middle.items().extractItem(0, 9, false).getCount() == 9 && middle.fluids().drain(900, FluidAction.EXECUTE).getAmount() == 900 && middle.gases().drain(900, GasAction.EXECUTE).getAmount() == 900, "Reloaded middle layers must allow extraction of all stored resources.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void breakingCenterDropsItemsAndReleasesGases(GameTestHelper helper) {
        verifyInventoryRemoval(helper, ORIGIN.offset(1, 1, 1));
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void breakingCornerDropsItemsAndReleasesGases(GameTestHelper helper) {
        verifyInventoryRemoval(helper, ORIGIN.offset(2, 2, 2));
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void towerThermoregulatorsSelectFractionationAndCondensation(GameTestHelper helper) {
        assembleTower(helper);
        BlockPos sourceOrigin = ORIGIN.below();
        BlockState heated = AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED);
        BlockState superheated = heated.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING);
        BlockState chilled = CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED);
        assertTowerThermalState(helper, 0, AirtightFractionationTowerMode.NONE);
        helper.setBlock(sourceOrigin, heated.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SMOULDERING));
        assertTowerThermalState(helper, 0, AirtightFractionationTowerMode.NONE);
        helper.setBlock(sourceOrigin, heated);
        assertTowerThermalState(helper, 1, AirtightFractionationTowerMode.FRACTIONATION);
        helper.setBlock(sourceOrigin, heated.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.FADING));
        assertTowerThermalState(helper, 1, AirtightFractionationTowerMode.FRACTIONATION);
        helper.setBlock(sourceOrigin.offset(2, 0, 2), superheated);
        assertTowerThermalState(helper, 4, AirtightFractionationTowerMode.FRACTIONATION);
        helper.setBlock(sourceOrigin.offset(2, 0, 2), Blocks.AIR);
        helper.setBlock(sourceOrigin.offset(1, 0, 0), chilled);
        assertTowerThermalState(helper, 0, AirtightFractionationTowerMode.NONE);
        helper.setBlock(sourceOrigin, Blocks.AIR);
        assertTowerThermalState(helper, -1, AirtightFractionationTowerMode.CONDENSATION);
        helper.setBlock(sourceOrigin.offset(1, 0, 0), chilled.setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.RIMING));
        assertTowerThermalState(helper, 0, AirtightFractionationTowerMode.NONE);
        for (BlockPos pos : BlockPos.betweenClosed(sourceOrigin, sourceOrigin.offset(2, 0, 2))) {
            helper.setBlock(pos, superheated);
        }
        assertTowerThermalState(helper, 27, AirtightFractionationTowerMode.FRACTIONATION);
        for (BlockPos pos : BlockPos.betweenClosed(sourceOrigin, sourceOrigin.offset(2, 0, 2))) {
            helper.setBlock(pos, chilled);
        }
        assertTowerThermalState(helper, -9, AirtightFractionationTowerMode.CONDENSATION);
        helper.setBlock(sourceOrigin.offset(-1, 0, 0), superheated);
        helper.setBlock(ORIGIN.offset(-1, 1, 0), superheated);
        assertTowerThermalState(helper, -9, AirtightFractionationTowerMode.CONDENSATION);
        for (BlockPos pos : BlockPos.betweenClosed(sourceOrigin, sourceOrigin.offset(2, 0, 2))) {
            helper.setBlock(pos, Blocks.AIR);
        }
        assertTowerThermalState(helper, 0, AirtightFractionationTowerMode.NONE);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void towerTemperatureSurvivesSynchronizationAndRefreshesAfterReload(GameTestHelper helper) {
        assembleTower(helper);
        helper.setBlock(ORIGIN.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
        assertTowerThermalState(helper, 3, AirtightFractionationTowerMode.FRACTIONATION);
        ServerLevel level = helper.getLevel();
        Provider provider = level.registryAccess();
        BlockPos center = helper.absolutePos(ORIGIN.offset(1, 0, 1));
        AirtightFractionationTowerBlockEntity controller = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().getBlockEntity(level, center);
        if (controller == null) {
            throw new NullPointerException("Expected the bottom center tower controller at " + center + '.');
        }

        CompoundTag saved = controller.saveWithFullMetadata(provider);
        AirtightFractionationTowerBlockEntity receiver = new AirtightFractionationTowerBlockEntity(controller.getType(), center, controller.getBlockState());
        receiver.handleUpdateTag(controller.getUpdateTag(provider), provider);
        helper.assertTrue(receiver.getRecipeTemperature() == 3 && receiver.getProcessingMode() == AirtightFractionationTowerMode.FRACTIONATION, "Client synchronization must restore temperature and derive the same mode.");
        receiver.handleUpdateTag(new CompoundTag(), provider);
        helper.assertTrue(receiver.getRecipeTemperature() == 0 && receiver.getProcessingMode() == AirtightFractionationTowerMode.NONE, "Cleared structure synchronization must reset temperature and mode.");
        BlockEntity restored = BlockEntity.loadStatic(center, controller.getBlockState(), saved, provider);
        if (restored == null) {
            throw new NullPointerException("Expected a restored tower controller at " + center + '.');
        }

        if (!(restored instanceof AirtightFractionationTowerBlockEntity restoredController)) {
            throw new IllegalStateException("Expected a fractionation tower block entity at " + center + '.');
        }

        helper.assertTrue(restoredController.getRecipeTemperature() == 3, "Saved temperature must survive restoration before the first world scan.");
        level.setBlockEntity(restoredController);
        helper.setBlock(ORIGIN.below(), CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED));
        assertTowerThermalState(helper, -1, AirtightFractionationTowerMode.CONDENSATION);
        level.destroyBlock(helper.absolutePos(ORIGIN.offset(2, 2, 2)), false);
        helper.assertTrue(restoredController.getRecipeTemperature() == 0 && restoredController.getProcessingMode() == AirtightFractionationTowerMode.NONE, "Disassembly must reset the former controller's thermal state.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void fractionationCommitsBothProductsOnlyAtCompletion(GameTestHelper helper) {
        assembleTower(helper);
        helper.setBlock(ORIGIN.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
        LayerPorts input = requireLayerPorts(helper, ORIGIN);
        LayerPorts middle = requireLayerPorts(helper, ORIGIN.above());
        LayerPorts top = requireLayerPorts(helper, ORIGIN.above(2));
        helper.assertValueEqual(input.fluids().fill(new FluidStack(CCBFluids.BRIMSTONE.get().getSource(), 100), FluidAction.EXECUTE), 100, "brimstone input fill");
        List<RecipeHolder<FractionationTowerRecipe>> recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(CCBRecipeTypes.FRACTIONATION_TOWER.getType());
        FractionationTowerRecipe recipe = recipes.stream().filter(holder -> holder.id().equals(CCBAPI.asResource("fractionation_tower/brimstone_fractionation"))).map(RecipeHolder::value).findFirst().orElseThrow(() -> new IllegalStateException("Expected the generated brimstone recipe to be loaded."));
        helper.assertTrue(recipe.validate().isEmpty(), "Invalid generated brimstone recipe: " + recipe.validate());
        helper.assertTrue(new FractionationTowerCraftPlanner(input.items(), input.fluids(), input.gases()).planInputs(recipe).isPresent(), "Generated brimstone input does not match stored fluid: " + input.fluids().getFluidInTank(0));
        tickTower(helper, 199);
        assertInsertionRejected(helper, middle);
        helper.assertTrue(input.fluids().getFluidInTank(0).getAmount() == 100 && middle.fluids().getFluidInTank(0).isEmpty() && top.gases().getGasInTank(0).isEmpty(), "Processing must reserve the original input until completion without partial products.");
        tickTower(helper, 1);
        helper.assertTrue(input.fluids().getFluidInTank(0).isEmpty(), "Completed fractionation must consume exactly 100 mB of brimstone.");
        FluidStack lava = middle.fluids().getFluidInTank(0);
        GasStack gas = top.gases().getGasInTank(0);
        helper.assertTrue(lava.is(Fluids.LAVA) && lava.getAmount() == 500, "The next layer must receive 500 mB of lava.");
        helper.assertTrue(gas.getGasType() == CCBGases.ULTRAWARM_AIR.get() && gas.getAmount() == 500, "The second layer above input must receive 500 GU of ultrawarm air.");
        assertInsertionRejected(helper, middle);
        tickTower(helper, 220);
        helper.assertTrue(lava.getAmount() == 500 && top.gases().getGasInTank(0).getAmount() == 500, "An empty input must not repeat the completed recipe.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void condensationUsesTheHighestLayersOfATallTower(GameTestHelper helper) {
        placeTank(helper, 3, 9);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.assertTrue(usePanel(helper, player, ORIGIN.above(8)).consumesAction(), "Expected a nine-layer tower to assemble.");
        helper.setBlock(ORIGIN.below(), CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED));
        LayerPorts input = requireLayerPorts(helper, ORIGIN.above(8));
        input.gases().fill(new GasStack(CCBGases.MOIST_AIR.get(), 10000), GasAction.EXECUTE);
        tickTower(helper, 99);
        helper.assertTrue(input.gases().getGasInTank(0).getAmount() == 10000, "Condensation must retain input through tick 99.");
        tickTower(helper, 1);
        GasStack air = requireLayerPorts(helper, ORIGIN.above(7)).gases().getGasInTank(0);
        FluidStack water = requireLayerPorts(helper, ORIGIN.above(6)).fluids().getFluidInTank(0);
        helper.assertTrue(input.gases().getGasInTank(0).isEmpty() && air.getGasType() == CCBGases.NATURAL_AIR.get() && air.getAmount() == 9800, "Condensation must consume top-layer moist air and output 9800 GU of natural air one layer below.");
        helper.assertTrue(water.is(Fluids.WATER) && water.getAmount() == 100, "Condensation must collect 100 mB of water two layers below input.");
        assertInsertionRejected(helper, requireLayerPorts(helper, ORIGIN.above(7)));
        assertInsertionRejected(helper, requireLayerPorts(helper, ORIGIN.above(6)));
        helper.assertTrue(requireLayerPorts(helper, ORIGIN).fluids().getFluidInTank(0).isEmpty(), "Unused lower layers must remain empty.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void temperatureAndBlockedOutputsPauseWithoutConsumingInputs(GameTestHelper helper) {
        assembleTower(helper);
        BlockState heat = AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING);
        helper.setBlock(ORIGIN.below(), heat);
        LayerPorts input = requireLayerPorts(helper, ORIGIN);
        LayerPorts middle = requireLayerPorts(helper, ORIGIN.above());
        input.fluids().fill(new FluidStack(CCBFluids.BRIMSTONE.get().getSource(), 100), FluidAction.EXECUTE);
        tickTower(helper, 80);
        helper.setBlock(ORIGIN.below(), Blocks.AIR);
        tickTower(helper, 200);
        helper.assertValueEqual(requireTowerController(helper).getUpdateTag(helper.getLevel().registryAccess()).getCompound("Crafting").getInt("Progress"), 80, "progress while heating is missing");
        helper.assertTrue("TEMPERATURE".equals(requireTowerController(helper).getUpdateTag(helper.getLevel().registryAccess()).getCompound("Crafting").getString("PauseReason")), "Missing heat must report a temperature pause.");
        helper.setBlock(ORIGIN.below(), heat);
        LayerPorts top = requireLayerPorts(helper, ORIGIN.above(2));
        top.fluids().fill(new FluidStack(Fluids.WATER, middle.fluids().getTankCapacity(0)), FluidAction.EXECUTE);
        middle = copyLayerInventory(helper, 2, 1);
        top.fluids().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        tickTower(helper, 200);
        helper.assertTrue(input.fluids().getFluidInTank(0).getAmount() == 100 && requireLayerPorts(helper, ORIGIN.above(2)).gases().getGasInTank(0).isEmpty(), "A blocked fluid output must prevent consumption and every other output.");
        helper.assertTrue("OUTPUT".equals(requireTowerController(helper).getUpdateTag(helper.getLevel().registryAccess()).getCompound("Crafting").getString("PauseReason")), "Blocked output must replace the temperature pause reason without changing progress.");
        middle.fluids().drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
        tickTower(helper, 119);
        helper.assertTrue(input.fluids().getFluidInTank(0).getAmount() == 100, "Resumed processing must retain its previous progress without completing early.");
        CompoundTag resumed = requireTowerController(helper).getUpdateTag(helper.getLevel().registryAccess()).getCompound("Crafting");
        helper.assertTrue(!resumed.getBoolean("Paused") && "OTHER".equals(resumed.getString("PauseReason")), "Resuming processing must clear the stale pause reason.");
        tickTower(helper, 1);
        helper.assertTrue(input.fluids().getFluidInTank(0).isEmpty() && middle.fluids().getFluidInTank(0).getAmount() == 500, "Restoring heat and output space must resume the remaining 120 ticks.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void wrongInputLayerAndSupercoolingRejectCondensation(GameTestHelper helper) {
        assembleTower(helper);
        BlockState cooling = CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED);
        helper.setBlock(ORIGIN.below(), cooling);
        LayerPorts bottom = requireLayerPorts(helper, ORIGIN);
        LayerPorts top = requireLayerPorts(helper, ORIGIN.above(2));
        bottom.gases().fill(new GasStack(CCBGases.MOIST_AIR.get(), 10000), GasAction.EXECUTE);
        tickTower(helper, 120);
        helper.assertTrue(bottom.gases().getGasInTank(0).getAmount() == 10000, "Condensation must not consume bottom-layer gas.");
        bottom.gases().drain(10000, GasAction.EXECUTE);
        top.gases().fill(new GasStack(CCBGases.MOIST_AIR.get(), 10000), GasAction.EXECUTE);
        helper.setBlock(ORIGIN.below().east(), cooling);
        helper.setBlock(ORIGIN.below().south(), cooling);
        tickTower(helper, 120);
        helper.assertTrue(top.gases().getGasInTank(0).getAmount() == 10000 && bottom.fluids().getFluidInTank(0).isEmpty(), "Superchilled conditions must not match the exact chilled recipe.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void craftingProgressSurvivesReloadAndDisassemblyClearsIt(GameTestHelper helper) {
        assembleTower(helper);
        helper.setBlock(ORIGIN.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
        requireLayerPorts(helper, ORIGIN).fluids().fill(new FluidStack(CCBFluids.BRIMSTONE.get().getSource(), 200), FluidAction.EXECUTE);
        tickTower(helper, 70);
        AirtightFractionationTowerBlockEntity controller = requireTowerController(helper);
        Provider provider = helper.getLevel().registryAccess();
        BlockEntity restored = BlockEntity.loadStatic(controller.getBlockPos(), controller.getBlockState(), controller.saveWithFullMetadata(provider), provider);
        if (restored == null) {
            throw new NullPointerException("Expected a restored tower controller at " + controller.getBlockPos() + '.');
        }

        helper.getLevel().setBlockEntity(restored);
        tickTower(helper, 130);
        helper.assertTrue(requireLayerPorts(helper, ORIGIN).fluids().getFluidInTank(0).getAmount() == 100, "Reloaded processing must finish using the saved 70 ticks.");
        tickTower(helper, 30);
        AirtightFractionationTowerBlockEntity reloadedController = requireTowerController(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(ORIGIN.above(2)), true);
        reloadedController.tick();
        helper.assertTrue(reloadedController.getUpdateTag(provider).getCompound("Crafting").isEmpty(), "Disassembly must erase the active recipe and progress.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void mixedInputsRespectLayerRequirementsAndRecipeReload(GameTestHelper helper) {
        assembleTower(helper);
        helper.setBlock(ORIGIN.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
        LayerPorts input = requireLayerPorts(helper, ORIGIN);
        input.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
        input.fluids().fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE);
        input.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), GasAction.EXECUTE);
        FractionationTowerRecipe tooTall = new Builder(CCBAPI.asResource("test/tower_tall")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(10).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(3, new GasStack(CCBGases.STEAM.get(), 50)).build();
        FractionationTowerRecipe mixed = new Builder(CCBAPI.asResource("test/tower_mixed")).require(Items.IRON_INGOT).require(Fluids.WATER, 100).require(CCBGases.NATURAL_AIR.get(), 100).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(10).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(2, new GasStack(CCBGases.STEAM.get(), 50)).build();
        try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(tooTall))) {
            tickTower(helper, 30);
            helper.assertValueEqual(input.items().getStackInSlot(0).getCount(), 2, "input count when required output layer is missing");
            scope.reload(List.of(mixed));
            tickTower(helper, 12);
            helper.assertTrue(input.fluids().getFluidInTank(0).getAmount() == 100, "Mixed inputs must remain intact before completion.");
            scope.reload(List.of());
            tickTower(helper, 1);
            helper.assertTrue(requireTowerController(helper).getUpdateTag(helper.getLevel().registryAccess()).getCompound("Crafting").isEmpty(), "Removing the active recipe must clear its progress without consuming resources.");
            scope.reload(List.of(mixed));
            tickTower(helper, 10);
            helper.assertTrue(input.items().getStackInSlot(0).getCount() == 1 && input.fluids().getFluidInTank(0).isEmpty() && input.gases().getGasInTank(0).isEmpty(), "Mixed processing must consume the exact item, fluid and gas inputs together.");
            helper.assertTrue(requireLayerPorts(helper, ORIGIN.above()).items().getStackInSlot(0).is(Items.GOLD_INGOT), "The first product layer must receive the item output.");
            helper.assertValueEqual(requireLayerPorts(helper, ORIGIN.above(2)).gases().getGasInTank(0).getAmount(), 50L, "second layer gas output");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void oppositeModesRestartProcessingWithoutChangingStoredResources(GameTestHelper helper) {
        assembleTower(helper);
        Provider provider = helper.getLevel().registryAccess();
        AirtightFractionationTowerBlockEntity controller = requireTowerController(helper);
        BlockState heated = AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING);
        BlockState chilled = CCBBlocks.BREEZE_COOLER_BLOCK.getDefaultState().setValue(BreezeCoolerBlock.FROST_LEVEL, FrostLevel.CHILLED);
        LayerPorts bottom = requireLayerPorts(helper, ORIGIN);
        LayerPorts top = requireLayerPorts(helper, ORIGIN.above(2));
        top.items().insertItem(0, new ItemStack(Items.DIAMOND, 7), false);
        LayerPorts middle = copyLayerInventory(helper, 2, 1);
        bottom.fluids().fill(new FluidStack(CCBFluids.BRIMSTONE.get().getSource(), 100), FluidAction.EXECUTE);
        top.gases().fill(new GasStack(CCBGases.MOIST_AIR.get(), 10000), GasAction.EXECUTE);
        FractionationTowerRecipe heating = new Builder(CCBAPI.asResource("test/tower_mode_heating")).require(new SizedFluidIngredient(FluidIngredient.of(CCBFluids.BRIMSTONE.get().getSource()), 100)).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(200).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(2, new ItemStack(Items.DIAMOND)).build();
        FractionationTowerRecipe cooling = new Builder(CCBAPI.asResource("test/tower_mode_cooling")).require(CCBGases.MOIST_AIR.get(), 10000).temperatureCondition(TemperatureCondition.CHILLED).duration(100).outputAtLayer(1, new ItemStack(Items.EMERALD)).outputAtLayer(2, new ItemStack(Items.DIAMOND)).build();
        try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(heating, cooling))) {
            helper.setBlock(ORIGIN.below(), heated);
            tickTower(helper, 80);
            helper.setBlock(ORIGIN.below(), heated.setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED));
            tickTower(helper, 10);
            CompoundTag underheated = controller.getUpdateTag(provider).getCompound("Crafting");
            helper.assertTrue(underheated.getInt("Progress") == 80 && underheated.getBoolean("Paused"), "Insufficient heat in the same mode must preserve fractionation progress.");
            helper.setBlock(ORIGIN.below(), Blocks.AIR);
            tickTower(helper, 10);
            helper.assertValueEqual(controller.getUpdateTag(provider).getCompound("Crafting").getInt("Progress"), 80, "progress without a temperature source");
            helper.setBlock(ORIGIN.below(), chilled);
            tickTower(helper, 1);
            CompoundTag condensation = controller.getUpdateTag(provider).getCompound("Crafting");
            helper.assertTrue(condensation.getInt("Duration") == 100 && condensation.getInt("Progress") == 1 && !condensation.getBoolean("Paused"), "Switching to condensation must start a fresh recipe without inheriting fractionation progress.");
            helper.setBlock(ORIGIN.offset(1, -1, 0), chilled);
            helper.setBlock(ORIGIN.offset(2, -1, 0), chilled);
            tickTower(helper, 10);
            CompoundTag overcooled = controller.getUpdateTag(provider).getCompound("Crafting");
            helper.assertTrue(overcooled.getInt("Progress") == 1 && overcooled.getBoolean("Paused"), "A mismatched cooling tier must preserve condensation progress.");
            helper.setBlock(ORIGIN.offset(1, -1, 0), Blocks.AIR);
            helper.setBlock(ORIGIN.offset(2, -1, 0), Blocks.AIR);
            tickTower(helper, 39);
            helper.setBlock(ORIGIN.below(), heated);
            tickTower(helper, 1);
            CompoundTag fractionation = controller.getUpdateTag(provider).getCompound("Crafting");
            helper.assertTrue(fractionation.getInt("Duration") == 200 && fractionation.getInt("Progress") == 1 && !fractionation.getBoolean("Paused"), "Switching back to fractionation must discard the previous progress of both modes.");
            helper.assertTrue(bottom.fluids().getFluidInTank(0).getAmount() == 100 && top.gases().getGasInTank(0).getAmount() == 10000, "Mode changes must not consume or relocate either mode's inputs.");
            helper.assertTrue(middle.items().getStackInSlot(0).getCount() == 7 && top.items().getStackInSlot(0).getCount() == 7, "Mode changes must preserve existing layer contents.");
            top.gases().drain(10000, GasAction.EXECUTE);
            helper.setBlock(ORIGIN.below(), chilled);
            tickTower(helper, 1);
            helper.assertTrue(controller.getUpdateTag(provider).getCompound("Crafting").isEmpty(), "Switching modes without a matching new input must clear the old processing state.");
            helper.assertValueEqual(bottom.fluids().getFluidInTank(0).getAmount(), 100, "unconsumed fractionation input after cancelling processing");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x12x5", timeoutTicks = 40)
    public static void processingProgressSynchronizesPauseAndCompletion(GameTestHelper helper) {
        assembleTower(helper);
        BlockState heat = AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING);
        helper.setBlock(ORIGIN.below(), heat);
        requireLayerPorts(helper, ORIGIN).fluids().fill(new FluidStack(CCBFluids.BRIMSTONE.get().getSource(), 100), FluidAction.EXECUTE);
        tickTower(helper, 100);
        AirtightFractionationTowerBlockEntity controller = requireTowerController(helper);
        Provider provider = helper.getLevel().registryAccess();
        CompoundTag update = controller.getUpdateTag(provider);
        CompoundTag crafting = update.getCompound("Crafting");
        helper.assertValueEqual(crafting.getInt("Duration"), 200, "synchronized recipe duration");
        helper.assertValueEqual(crafting.getInt("Progress"), 100, "synchronized progress");
        helper.assertTrue(!crafting.getBoolean("Paused"), "An advancing recipe must not be marked paused.");
        AirtightFractionationTowerBlockEntity receiver = new AirtightFractionationTowerBlockEntity(controller.getType(), controller.getBlockPos(), controller.getBlockState());
        receiver.handleUpdateTag(update, provider);
        CompoundTag received = receiver.getUpdateTag(provider).getCompound("Crafting");
        helper.assertTrue(received.getInt("Progress") == 100 && received.getInt("Duration") == 200, "Synchronization must preserve both progress and duration for the client progress bar.");
        helper.setBlock(ORIGIN.below(), Blocks.AIR);
        tickTower(helper, 10);
        receiver.handleUpdateTag(controller.getUpdateTag(provider), provider);
        CompoundTag paused = receiver.getUpdateTag(provider).getCompound("Crafting");
        helper.assertTrue(paused.getBoolean("Paused") && paused.getInt("Progress") == 100, "Pause synchronization must retain the last progress.");
        helper.assertTrue("TEMPERATURE".equals(paused.getString("PauseReason")), "Pause synchronization must preserve the temperature reason.");
        BlockPos middlePos = helper.absolutePos(ORIGIN.offset(1, 1, 1));
        BlockEntity middle = helper.getLevel().getBlockEntity(middlePos);
        if (middle == null) {
            throw new NullPointerException("Expected a tower layer controller at " + middlePos + '.');
        }

        CompoundTag savedMiddle = middle.saveWithFullMetadata(provider);
        helper.getLevel().removeBlockEntity(middlePos);
        tickTower(helper, 1);
        receiver.handleUpdateTag(controller.getUpdateTag(provider), provider);
        CompoundTag unavailable = receiver.getUpdateTag(provider).getCompound("Crafting");
        helper.assertTrue("STRUCTURE".equals(unavailable.getString("PauseReason")) && unavailable.getBoolean("Paused") && unavailable.getInt("Progress") == 100, "An unavailable layer must synchronize a structure pause while retaining progress.");
        BlockEntity restoredMiddle = BlockEntity.loadStatic(middlePos, middle.getBlockState(), savedMiddle, provider);
        if (restoredMiddle == null) {
            throw new NullPointerException("Failed to restore a tower layer controller at " + middlePos + '.');
        }

        helper.getLevel().setBlockEntity(restoredMiddle);
        tickTower(helper, 1);
        receiver.handleUpdateTag(controller.getUpdateTag(provider), provider);
        helper.assertTrue("TEMPERATURE".equals(receiver.getUpdateTag(provider).getCompound("Crafting").getString("PauseReason")), "Restoring the layer must replace the structure pause with the remaining temperature reason.");
        helper.setBlock(ORIGIN.below(), heat);
        tickTower(helper, 100);
        receiver.handleUpdateTag(controller.getUpdateTag(provider), provider);
        helper.assertTrue(receiver.getUpdateTag(provider).getCompound("Crafting").isEmpty(), "Completion must synchronize cleared processing state; server=" + controller.getUpdateTag(provider).getCompound("Crafting") + ", receiver=" + receiver.getUpdateTag(provider).getCompound("Crafting"));
        helper.succeed();
    }

    private static void verifyOutputRollback(GameTestHelper helper, boolean throwOnOutput) {
        assembleTower(helper);
        helper.setBlock(ORIGIN.below(), AllBlocks.BLAZE_BURNER.getDefaultState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.SEETHING));
        LayerPorts input = requireLayerPorts(helper, ORIGIN);
        LayerPorts middle = requireLayerPorts(helper, ORIGIN.above());
        LayerPorts top = requireLayerPorts(helper, ORIGIN.above(2));
        if (!(middle.items() instanceof SmartInventory middleItems) || !(top.items() instanceof SmartInventory topItems)) {
            throw new IllegalStateException("Expected smart item inventories for the tower rollback fixture.");
        }

        input.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 2), false);
        input.fluids().fill(new FluidStack(Fluids.WATER, 200), FluidAction.EXECUTE);
        input.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 2000), GasAction.EXECUTE);
        long pressureBefore = input.gases().getPressureCompartment(0).getPressurePa();
        FractionationTowerRecipe recipe = new Builder(CCBAPI.asResource("test/tower_rollback")).require(Items.IRON_INGOT).require(Fluids.WATER, 100).require(CCBGases.NATURAL_AIR.get(), 1000).temperatureCondition(TemperatureCondition.SUPERHEATED).duration(2).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(2, new ItemStack(Items.DIAMOND)).build();
        RuntimeException failure = new IllegalStateException("Intentional tower output failure.");
        int[] attempts = {0};
        middleItems.whenContentsChanged(slot -> {
            if (attempts[0] != 0 || !middleItems.getStackInSlot(slot).is(Items.GOLD_INGOT)) {
                return;
            }

            attempts[0]++;
            helper.assertTrue(input.items().getStackInSlot(0).getCount() == 1 && input.fluids().getFluidInTank(0).getAmount() == 100 && input.gases().getGasInTank(0).getAmount() == 1000, "Failure injection must run after all three input types have been consumed.");
            if (throwOnOutput) {
                throw failure;
            }

            topItems.withMaxStackSize(0);
        });
        try (RecipeIndexTestScope ignored = new RecipeIndexTestScope(helper.getLevel(), List.of(recipe))) {
            tickTower(helper, 1);
            boolean threw = false;
            try {
                tickTower(helper, 1);
            }
            catch (RuntimeException exception) {
                if (exception != failure) {
                    throw exception;
                }

                threw = true;
            }
            helper.assertTrue(threw == throwOnOutput && attempts[0] == 1, "The output failure must occur once during execution after successful simulation.");
            helper.assertTrue(input.items().getStackInSlot(0).getCount() == 2 && input.fluids().getFluidInTank(0).getAmount() == 200 && input.gases().getGasInTank(0).getAmount() == 2000, "Rollback must restore the exact mixed input quantities.");
            helper.assertValueEqual(input.gases().getPressureCompartment(0).getPressurePa(), pressureBefore, "input gas pressure after rollback");
            helper.assertTrue(middleItems.isEmpty() && topItems.isEmpty(), "Rollback must remove every partially committed product.");
            assertInsertionRejected(helper, middle);
            topItems.withMaxStackSize(64);
            tickTower(helper, 1);
            helper.assertTrue(input.items().getStackInSlot(0).getCount() == 1 && input.fluids().getFluidInTank(0).getAmount() == 100 && input.gases().getGasInTank(0).getAmount() == 1000, "Retry must consume each input exactly once.");
            helper.assertTrue(middleItems.getStackInSlot(0).is(Items.GOLD_INGOT) && middleItems.getStackInSlot(0).getCount() == 1 && topItems.getStackInSlot(0).is(Items.DIAMOND) && topItems.getStackInSlot(0).getCount() == 1, "Retry must create each product exactly once.");
            assertInsertionRejected(helper, middle);
        }
        finally {
            middleItems.whenContentsChanged(slot -> {});
            topItems.withMaxStackSize(64);
        }
        helper.succeed();
    }

    private static void assertInsertionRejected(GameTestHelper helper, LayerPorts ports) {
        for (boolean simulate : new boolean[]{true, false}) {
            FluidAction fluidAction = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            GasAction gasAction = simulate ? GasAction.SIMULATE : GasAction.EXECUTE;
            helper.assertValueEqual(ports.items().insertItem(0, new ItemStack(Items.DIAMOND), simulate).getCount(), 1, "rejected middle-layer item insertion");
            helper.assertValueEqual(ports.fluids().fill(new FluidStack(Fluids.WATER, 100), fluidAction), 0, "rejected middle-layer fluid insertion");
            helper.assertValueEqual(ports.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), gasAction), 0L, "rejected middle-layer gas insertion");
            helper.assertValueEqual(ports.gases().getPressureCompartment(0).fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), gasAction), 0L, "rejected middle-layer pressure compartment insertion");
        }
    }

    private static LayerPorts copyLayerInventory(GameTestHelper helper, int sourceLayer, int targetLayer) {
        return copyLayerInventory(helper, ORIGIN, sourceLayer, targetLayer);
    }

    private static LayerPorts copyLayerInventory(GameTestHelper helper, BlockPos origin, int sourceLayer, int targetLayer) {
        ServerLevel level = helper.getLevel();
        Provider provider = level.registryAccess();
        BlockPos sourcePos = helper.absolutePos(origin.offset(1, sourceLayer, 1));
        BlockPos targetPos = helper.absolutePos(origin.offset(1, targetLayer, 1));
        BlockEntity source = level.getBlockEntity(sourcePos);
        BlockEntity target = level.getBlockEntity(targetPos);
        if (source == null || target == null) {
            throw new NullPointerException("Expected tower layer controllers at " + sourcePos + " and " + targetPos + '.');
        }

        CompoundTag saved = target.saveWithFullMetadata(provider);
        saved.put("LayerInventory", source.saveWithFullMetadata(provider).getCompound("LayerInventory").copy());
        BlockEntity restored = BlockEntity.loadStatic(targetPos, target.getBlockState(), saved, provider);
        if (restored == null) {
            throw new NullPointerException("Failed to reload tower layer contents at " + targetPos + '.');
        }

        level.setBlockEntity(restored);
        return requireLayerPorts(helper, origin.above(targetLayer));
    }

    private static AirtightFractionationTowerBlockEntity requireTowerController(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(ORIGIN.offset(1, 0, 1));
        AirtightFractionationTowerBlockEntity controller = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().getBlockEntity(helper.getLevel(), center);
        if (controller == null) {
            throw new NullPointerException("Expected the tower processing controller at " + center + '.');
        }

        return controller;
    }

    private static void tickTower(GameTestHelper helper, int ticks) {
        AirtightFractionationTowerBlockEntity controller = requireTowerController(helper);
        for (int tick = 0; tick < ticks; tick++) {
            controller.tick();
        }
    }

    private static void assertTowerThermalState(GameTestHelper helper, float temperature, AirtightFractionationTowerMode mode) {
        ServerLevel level = helper.getLevel();
        Provider provider = level.registryAccess();
        BlockPos center = helper.absolutePos(ORIGIN.offset(1, 0, 1));
        AirtightFractionationTowerBlockEntity controller = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().getBlockEntity(level, center);
        if (controller == null) {
            throw new NullPointerException("Expected the bottom center tower controller at " + center + '.');
        }

        controller.tick();
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(2, 2, 2))) {
            BlockPos absolutePos = helper.absolutePos(pos);
            AirtightFractionationTowerBlockEntity member = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().getBlockEntity(level, absolutePos);
            if (member == null) {
                throw new NullPointerException("Expected a tower member at " + absolutePos + '.');
            }

            float actualTemperature = member.getRecipeTemperature();
            AirtightFractionationTowerMode actualMode = member.getProcessingMode();
            helper.assertTrue(actualTemperature == temperature && actualMode == mode, "Every member must use the bottom center thermal state: expected temperature=" + temperature + ", mode=" + mode + ", actual temperature=" + actualTemperature + ", mode=" + actualMode + '.');
            helper.assertTrue(member.getUpdateTag(provider).contains("StructureManager") == absolutePos.equals(center), "Only the bottom center may persist and synchronize tower temperature.");
        }
    }

    private static void verifyInventoryRemoval(GameTestHelper helper, BlockPos removedPos) {
        assembleTower(helper);
        LayerPorts[] layers = new LayerPorts[3];
        for (int layer = 0; layer < layers.length; layer++) {
            if (layer == 1) {
                layers[layer] = copyLayerInventory(helper, 0, layer);
                continue;
            }

            LayerPorts ports = requireLayerPorts(helper, ORIGIN.above(layer));
            layers[layer] = ports;
            ports.items().insertItem(0, new ItemStack(Items.DIAMOND, 12), false);
            ports.items().insertItem(ports.items().getSlots() - 1, new ItemStack(Items.EMERALD, 7), false);
            ports.fluids().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
            ports.gases().fill(new GasStack(CCBGases.MOIST_AIR.get(), 1000), GasAction.EXECUTE);
        }
        ServerLevel level = helper.getLevel();
        Pig target = helper.spawn(EntityType.PIG, ORIGIN.offset(1, 1, 1));
        target.igniteForSeconds(30);
        level.destroyBlock(helper.absolutePos(removedPos), true);
        helper.assertTrue(!target.isOnFire(), "Tower disassembly must apply stored moist air release effects.");
        for (LayerPorts ports : layers) {
            helper.assertTrue(ports.items().getStackInSlot(0).isEmpty() && ports.items().getStackInSlot(ports.items().getSlots() - 1).isEmpty() && ports.fluids().getFluidInTank(0).isEmpty() && ports.gases().getGasInTank(0).isEmpty(), "Disassembly must empty every layer, including previously cached handlers.");
            helper.assertTrue(!ports.items().insertItem(0, new ItemStack(Items.DIAMOND), false).isEmpty() && ports.fluids().fill(new FluidStack(Fluids.WATER, 100), FluidAction.EXECUTE) == 0 && ports.gases().fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), GasAction.EXECUTE) == 0, "Detached handlers must reject new insertion.");
            helper.assertTrue(ports.gases().getPressureCompartment(0).fill(new GasStack(CCBGases.NATURAL_AIR.get(), 100), GasAction.EXECUTE) == 0, "Detached gas pressure compartments must reject insertion.");
        }
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(2, 2, 2))) {
            if (pos.equals(removedPos)) {
                continue;
            }

            BlockPos absolutePos = helper.absolutePos(pos);
            helper.assertTrue(level.getCapability(ItemHandler.BLOCK, absolutePos, Direction.NORTH) == null && level.getCapability(FluidHandler.BLOCK, absolutePos, Direction.NORTH) == null, "Restored tanks must not retain tower item or fluid capabilities.");
            GasHandler gas = level.getCapability(GasCapabilities.BLOCK, absolutePos, Direction.NORTH);
            if (gas == null) {
                throw new NullPointerException("Expected gas capability on a restored tank at " + absolutePos + '.');
            }

            helper.assertTrue(gas.getGasInTank(0).isEmpty(), "Restored tanks must not inherit tower gases.");
        }
        level.destroyBlock(helper.absolutePos(ORIGIN), true);
        AABB bounds = new AABB(helper.absolutePos(ORIGIN)).expandTowards(3, 3, 3).inflate(1);
        int panels = 0;
        int diamonds = 0;
        int emeralds = 0;
        AABB dropBounds = new AABB(helper.absolutePos(removedPos));
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = item.getItem();
            if (stack.is(Items.DIAMOND) || stack.is(Items.EMERALD) || stack.is(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get())) {
                helper.assertTrue(dropBounds.contains(item.position()), "Stored items and the instrument panel must drop at the destroyed tower block.");
            }
            if (stack.is(Items.DIAMOND)) {
                diamonds += stack.getCount();
            }
            if (stack.is(Items.EMERALD)) {
                emeralds += stack.getCount();
            }
            if (!stack.is(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get())) {
                continue;
            }

            panels += stack.getCount();
        }
        helper.assertTrue(panels == 1, "Disassembly with stored contents must return exactly one instrument panel.");
        helper.assertValueEqual(diamonds, 36, "diamond drops from all three layers without duplication");
        helper.assertValueEqual(emeralds, 21, "last-slot emerald drops from all three layers without duplication");
        helper.succeed();
    }

    private static void assembleTower(GameTestHelper helper) {
        placeTank(helper, 3, 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        helper.assertTrue(usePanel(helper, player, ORIGIN).consumesAction(), "Expected an empty tank group to assemble for the layer inventory test.");
    }

    private static LayerPorts requireLayerPorts(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        BlockPos absolutePos = helper.absolutePos(pos);
        IItemHandler items = level.getCapability(ItemHandler.BLOCK, absolutePos, Direction.NORTH);
        IFluidHandler fluids = level.getCapability(FluidHandler.BLOCK, absolutePos, Direction.NORTH);
        GasHandler gases = level.getCapability(GasCapabilities.BLOCK, absolutePos, Direction.NORTH);
        if (items == null || fluids == null || gases == null) {
            throw new NullPointerException("Expected all three tower capabilities at " + absolutePos + '.');
        }

        BlockEntity tower = level.getBlockEntity(absolutePos);
        if (tower == null) {
            throw new NullPointerException("Expected a tower block entity at " + absolutePos + '.');
        }

        if (!(gases instanceof GasStorageHandler storage) || !(tower instanceof GasInventoryIdentifierProvider provider)) {
            throw new IllegalStateException("Expected shared gas storage and identity at " + absolutePos + '.');
        }

        InventoryIdentifier identifier = provider.getGasInventoryIdentifier(Direction.NORTH);
        if (identifier == null) {
            throw new NullPointerException("Expected the tower layer gas identity at " + absolutePos + '.');
        }

        return new LayerPorts(items, fluids, storage, identifier);
    }

    private static void verifyAssemblyAndRemoval(GameTestHelper helper, int height) {
        placeTank(helper, 3, height);
        ServerLevel level = helper.getLevel();
        Provider provider = level.registryAccess();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get()));
        Property<?> top = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().getStateDefinition().getProperty("top");
        Property<?> bottom = CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get().getStateDefinition().getProperty("bottom");
        if (top == null || bottom == null) {
            throw new NullPointerException("Expected top and bottom properties on the airtight fractionation tower block.");
        }

        BlockPos clickedPos = ORIGIN.offset(2, height - 1, 2);
        helper.assertTrue(usePanel(helper, player, clickedPos).consumesAction(), "Valid empty tank must assemble from any member.");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Assembly must consume one instrument panel.");
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(2, height - 1, 2))) {
            BlockPos absolutePos = helper.absolutePos(pos);
            BlockState towerState = level.getBlockState(absolutePos);
            helper.assertTrue(towerState.is(CCBBlocks.AIRTIGHT_FRACTIONATION_TOWER_BLOCK.get()), "Every tank member must convert into a tower block.");
            int layer = pos.getY() - ORIGIN.getY();
            helper.assertTrue(Boolean.valueOf(layer == height - 1).equals(towerState.getValue(top)), "Only the highest tower layer must be marked as top.");
            helper.assertTrue(Boolean.valueOf(layer == 0).equals(towerState.getValue(bottom)), "Only the lowest tower layer must be marked as bottom.");
            BlockEntity tower = level.getBlockEntity(absolutePos);
            if (tower == null) {
                throw new NullPointerException("Expected a fractionation tower block entity at " + absolutePos + '.');
            }

            CompoundTag update = tower.getUpdateTag(provider);
            BlockEntity receiver = tower.getType().create(absolutePos, towerState);
            if (receiver == null) {
                throw new NullPointerException("Failed to create fractionation tower synchronization receiver at " + absolutePos + '.');
            }

            receiver.handleUpdateTag(update, provider);
            CompoundTag synchronizedTag = receiver.getUpdateTag(provider);
            helper.assertTrue(synchronizedTag.getLong("TowerOrigin") == helper.absolutePos(ORIGIN).asLong() && synchronizedTag.getInt("TowerHeight") == height, "Create synchronization must preserve the tower origin and height.");
            receiver.handleUpdateTag(new CompoundTag(), provider);
            helper.assertTrue(!receiver.getUpdateTag(provider).contains("TowerOrigin"), "Empty structure synchronization must clear the previous tower identity.");
            CompoundTag saved = tower.saveWithFullMetadata(provider);
            helper.assertTrue(saved.getLong("TowerOrigin") == helper.absolutePos(ORIGIN).asLong() && saved.getInt("TowerHeight") == height, "Every tower member must retain the same structure identity.");
            BlockEntity restored = BlockEntity.loadStatic(absolutePos, towerState, saved, provider);
            if (restored == null) {
                throw new NullPointerException("Failed to reload fractionation tower block entity at " + absolutePos + '.');
            }

            level.setBlockEntity(restored);
        }
        level.destroyBlock(helper.absolutePos(clickedPos), true);
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(2, height - 1, 2))) {
            if (pos.equals(clickedPos)) {
                continue;
            }

            helper.assertTrue(level.getBlockState(helper.absolutePos(pos)).is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()), "Every remaining tower member must revert to an airtight tank.");
        }
        level.destroyBlock(helper.absolutePos(ORIGIN), true);
        int panels = 0;
        int tanks = 0;
        AABB bounds = new AABB(helper.absolutePos(ORIGIN)).expandTowards(3, height, 3).inflate(1);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = item.getItem();
            if (stack.is(CCBItems.AIRTIGHT_FRACTIONATION_TOWER_INSTRUMENT_PANEL.get())) {
                panels += stack.getCount();
            }
            if (!stack.is(CCBBlocks.AIRTIGHT_TANK_BLOCK.asItem())) {
                continue;
            }

            tanks += stack.getCount();
        }
        helper.assertTrue(panels == 1, "Removing multiple blocks must return exactly one instrument panel.");
        helper.assertTrue(tanks == 2, "Each destroyed block must drop one airtight tank.");
        helper.succeed();
    }

    private static AirtightTankBlockEntity placeTank(GameTestHelper helper, int width, int height) {
        return placeTank(helper, ORIGIN, width, height);
    }

    private static AirtightTankBlockEntity placeTank(GameTestHelper helper, BlockPos origin, int width, int height) {
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(width - 1, height - 1, width - 1))) {
            helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState());
        }
        BlockPos absoluteOrigin = helper.absolutePos(origin);
        AirtightTankBlockEntity tank = CCBBlocks.AIRTIGHT_TANK_BLOCK.get().getBlockEntity(helper.getLevel(), absoluteOrigin);
        if (tank == null) {
            throw new NullPointerException("Expected an airtight tank at " + absoluteOrigin + '.');
        }

        GasTankMultiblockConnectivity.formMultiblock(tank, helper.getLevel());
        return tank;
    }

    private static InteractionResult usePanel(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolutePos = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.NORTH, absolutePos, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static void assertTankBlocks(GameTestHelper helper, int width, int height) {
        for (BlockPos pos : BlockPos.betweenClosed(ORIGIN, ORIGIN.offset(width - 1, height - 1, width - 1))) {
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).is(CCBBlocks.AIRTIGHT_TANK_BLOCK.get()), "Rejected assembly must preserve every airtight tank block.");
        }
    }

    private record LayerPorts(IItemHandler items, IFluidHandler fluids, GasStorageHandler gases, InventoryIdentifier identifier) {
    }
}
