package net.ty.createcraftedbeginning.gametests.gas.mounted;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.MountedStorageSyncPacket;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.mounted.CargoTrackedMountedGasStorageWrapper;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorage;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageAccess;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageSyncPacket;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageType;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MountedGasStorageGameTests {
    private static final BlockPos TANK_POS = new BlockPos(1, 1, 1);

    private MountedGasStorageGameTests() {
    }

    @SuppressWarnings("DataFlowIssue")
    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void mountedGasSurvivesSaveLoadAndUnmount(GameTestHelper helper) {
        MountedStorageManager manager = new MountedStorageManager();
        AirtightTankBlockEntity tank = mountTank(helper, manager);
        manager.initialize();
        MountedGasStorageWrapper gases = ((MountedGasStorageAccess) manager).ccb$getGasStorage();
        helper.assertValueEqual(gases.getGasInTank(0).getAmount(), 500L, "Mounted amount");
        gases.drain(123, GasAction.EXECUTE);
        helper.assertValueEqual(tank.getTankInventory().getGasStack().getAmount(), 500L, "World tank before unmount");
        CompoundTag saved = new CompoundTag();
        ServerLevel level = helper.getLevel();
        manager.write(saved, level.registryAccess(), false);
        MountedStorageManager restored = new MountedStorageManager();
        restored.read(saved, level.registryAccess(), false, null);
        helper.assertValueEqual(((MountedGasStorageAccess) restored).ccb$getGasStorage().getGasInTank(0).getAmount(), 377L, "Restored amount");
        restored.initialize();
        restored.unmount(level, new StructureBlockInfo(TANK_POS, tank.getBlockState(), null), tank.getBlockPos(), tank);
        helper.assertValueEqual(tank.getTankInventory().getGasStack().getAmount(), 377L, "Unmounted amount");
        helper.succeed();
    }

    @SuppressWarnings("DataFlowIssue")
    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void createAndGasSyncKeepEachOthersStorage(GameTestHelper helper) {
        MountedStorageManager manager = new MountedStorageManager();
        AirtightTankBlockEntity tank = mountTank(helper, manager);
        BlockPos chestPos = new BlockPos(3, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 2));
        ServerLevel level = helper.getLevel();
        manager.addBlock(level, chest.getBlockState(), chest.getBlockPos(), chestPos, chest);
        manager.initialize();
        MountedItemStorage itemStorage = manager.getAllItemStorages().get(chestPos);
        helper.assertTrue(itemStorage != null, "Chest was not mounted");
        if (itemStorage == null) {
            throw new NullPointerException("Chest was not mounted.");
        }

        MountedGasStorageAccess gasManager = (MountedGasStorageAccess) manager;
        MountedGasStorage original = gasManager.ccb$getGasStorage().storages.get(TANK_POS);
        Contraption structure = new BearingContraption() {
            @Override
            public BlockEntity getBlockEntityClientSide(BlockPos pos) {
                return tank;
            }
        };
        ControlledContraptionEntity entity = new ControlledContraptionEntity(AllEntityTypes.CONTROLLED_CONTRAPTION.get(), helper.getLevel()) {
            @Override
            public Contraption getContraption() {
                return structure;
            }
        };
        manager.handleSync(new MountedStorageSyncPacket(entity.getId(), Map.of(), Map.of()), entity);
        helper.assertTrue(gasManager.ccb$getGasStorage().storages.get(TANK_POS) == original, "Create reset replaced existing gas storage");
        tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 700)).requireAccepted();
        MountedGasStorage replacement = MountedGasStorageType.REGISTRY.get(tank.getBlockState().getBlock()).mount(helper.getLevel(), tank.getBlockState(), tank.getBlockPos(), tank);
        tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 100)).requireAccepted();
        gasManager.ccb$handleGasStorageSync(new MountedGasStorageSyncPacket(entity.getId(), Map.of(), Map.of(), Map.of(TANK_POS, replacement)), entity);
        helper.assertValueEqual(tank.getTankInventory().getGasStack().getAmount(), 700L, "Gas afterSync callback amount");
        manager.handleSync(new MountedStorageSyncPacket(entity.getId(), Map.of(), Map.of()), entity);
        helper.assertTrue(gasManager.ccb$getGasStorage().storages.get(TANK_POS) == replacement, "Later Create sync discarded gas replacement");
        helper.assertTrue(manager.getAllItemStorages().get(chestPos) == itemStorage, "Gas sync replaced mounted item storage");
        helper.assertValueEqual(itemStorage.getStackInSlot(0).getCount(), 2, "Items after interleaved sync");
        helper.succeed();
    }

    @SuppressWarnings("DataFlowIssue")
    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void dirtyGasWaitsForTheSharedSyncCooldown(GameTestHelper helper) {
        MountedStorageManager manager = new MountedStorageManager();
        mountTank(helper, manager);
        manager.initialize();
        MountedGasStorageWrapper gases = ((MountedGasStorageAccess) manager).ccb$getGasStorage();
        SyncedMountedStorage storage = (SyncedMountedStorage) gases.storages.get(TANK_POS);
        ControlledContraptionEntity entity = new ControlledContraptionEntity(AllEntityTypes.CONTROLLED_CONTRAPTION.get(), helper.getLevel());
        gases.drain(10, GasAction.EXECUTE);
        helper.assertTrue(storage.isDirty(), "Changed gas was not marked dirty");
        manager.tick(entity);
        helper.assertTrue(!storage.isDirty(), "Sent gas was not marked clean");
        gases.drain(10, GasAction.EXECUTE);
        for (int tick = 0; tick < 7; tick++) {
            manager.tick(entity);
        }
        helper.assertTrue(storage.isDirty(), "Gas was synced before cooldown expired");
        manager.tick(entity);
        helper.assertTrue(!storage.isDirty(), "Gas did not sync after cooldown expired");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void clientSnapshotOmitsUnsyncedGasButKeepsInteractionPositions(GameTestHelper helper) {
        MountedStorageManager manager = new MountedStorageManager();
        mountTank(helper, manager);
        BlockPos creativePos = new BlockPos(3, 1, 1);
        helper.setBlock(creativePos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.getDefaultState());
        BlockEntity creative = helper.getBlockEntity(creativePos);
        ServerLevel level = helper.getLevel();
        manager.addBlock(level, creative.getBlockState(), creative.getBlockPos(), creativePos, creative);
        manager.initialize();
        CompoundTag saved = new CompoundTag();
        CompoundTag client = new CompoundTag();
        manager.write(saved, level.registryAccess(), false);
        manager.write(client, level.registryAccess(), true);
        helper.assertValueEqual(saved.getList("gases", Tag.TAG_COMPOUND).size(), 2, "Saved gas storages");
        helper.assertValueEqual(client.getList("gases", Tag.TAG_COMPOUND).size(), 1, "Synced gas storages");
        helper.assertValueEqual(client.getList("interactable_positions", Tag.TAG_COMPOUND).size(), 2, "Interactable gas positions");
        manager.write(client, level.registryAccess(), true);
        helper.assertValueEqual(client.getList("interactable_positions", Tag.TAG_COMPOUND).size(), 2, "Repeated interaction positions");
        helper.succeed();
    }

    @SuppressWarnings("DataFlowIssue")
    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void trainWrapperTracksExecutedGasChanges(GameTestHelper helper) {
        TrainCargoManager manager = new TrainCargoManager();
        mountTank(helper, manager);
        manager.initialize();
        MountedGasStorageWrapper gases = ((MountedGasStorageAccess) manager).ccb$getGasStorage();
        helper.assertTrue(gases instanceof CargoTrackedMountedGasStorageWrapper, "Train gas wrapper was lost");
        int version = manager.getVersion();
        gases.drain(100, GasAction.SIMULATE);
        helper.assertValueEqual(manager.getVersion(), version, "Version after simulated drain");
        gases.drain(100, GasAction.EXECUTE);
        helper.assertValueEqual(manager.getVersion(), version + 1, "Version after executed drain");
        manager.tickIdleCargoTracker();
        gases.getPressureCompartment(0).fill(new GasStack(CCBGases.NATURAL_AIR.get(), 50), GasAction.EXECUTE);
        helper.assertValueEqual(manager.getVersion(), version + 2, "Version after compartment fill");
        helper.assertValueEqual(manager.getTicksSinceLastExchange(), 0, "Train idle timer after exchange");
        helper.succeed();
    }

    private static AirtightTankBlockEntity mountTank(GameTestHelper helper, MountedStorageManager manager) {
        helper.setBlock(TANK_POS, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState());
        AirtightTankBlockEntity tank = helper.getBlockEntity(TANK_POS);
        tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), 500)).requireAccepted();
        manager.addBlock(helper.getLevel(), tank.getBlockState(), tank.getBlockPos(), TANK_POS, tank);
        return tank;
    }
}
