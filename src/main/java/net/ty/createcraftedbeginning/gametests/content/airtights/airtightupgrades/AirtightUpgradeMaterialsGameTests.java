package net.ty.createcraftedbeginning.gametests.content.airtights.airtightupgrades;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.GogglesUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.VisionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HandheldDrillFilterButton;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.HarvestOptimizationUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightItemUpgrades;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu.InventoryHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeMaterials;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeMaterialsSyncPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirtightUpgradeMaterialsGameTests {
    private AirtightUpgradeMaterialsGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void creativeInstallationBypassesMaterialsButPreservesUpgradeValidation(GameTestHelper helper) {
        List<Item> equipmentItems = List.of(CCBItems.AIRTIGHT_HANDHELD_DRILL.get(), CCBItems.AIRTIGHT_HELMET.get(), CCBItems.AIRTIGHT_CHESTPLATE.get(), CCBItems.AIRTIGHT_LEGGINGS.get(), CCBItems.AIRTIGHT_BOOTS.get());
        ResourceLocation unknownUpgradeId = CCBAPI.asResource("unknown_upgrade");
        for (GameType gameType : List.of(GameType.CREATIVE, GameType.SURVIVAL)) {
            Player player = helper.makeMockPlayer(gameType);
            boolean creative = gameType == GameType.CREATIVE;
            Inventory inventory = player.getInventory();
            for (Item item : equipmentItems) {
                ItemStack equipment = new ItemStack(item);
                player.setItemInHand(InteractionHand.MAIN_HAND, equipment);
                AirtightUpgradableMenu menu = (AirtightUpgradableMenu) ((MenuProvider) item).createMenu(1, inventory, player);
                if (menu == null) {
                    throw new NullPointerException("Expected an airtight upgrade menu for '" + item + "'.");
                }

                List<AirtightUpgrade> upgrades = AirtightItemUpgrades.getAllUpgrades(equipment).stream().filter(upgrade -> !upgrade.startsInstalled()).toList();
                AirtightUpgrade first = upgrades.getFirst();
                AirtightUpgrade second = upgrades.get(1);
                ResourceLocation firstId = first.getID();
                ResourceLocation secondId = second.getID();
                InventoryHandler materials = menu.getMenuInventory();
                helper.assertTrue(menu.tryInstallUpgrade(firstId) == creative, "Only creative players may install without materials");
                helper.assertTrue(!menu.tryInstallUpgrade(unknownUpgradeId), "Creative mode must not allow unknown upgrade IDs");
                materials.setStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX, new ItemStack(Items.DIRT));
                helper.assertTrue(menu.tryInstallUpgrade(secondId) == creative, "Only creative players may ignore mismatched materials");
                helper.assertTrue(materials.getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX).is(Items.DIRT), "Bypassed or rejected installation must preserve slot contents");
                if (creative) {
                    helper.assertTrue(!menu.tryInstallUpgrade(firstId), "Creative mode must not install the same upgrade twice");
                    AirtightUpgradeStatus installedStatus = menu.getStatus(first);
                    helper.assertTrue(installedStatus.isInstalled() && installedStatus.isEnabled(), "Creative installation must immediately enable the upgrade");
                    helper.assertTrue(menu.getStatus(second).isEnabled(), "Creative installation with mismatched materials must immediately enable the upgrade");
                    continue;
                }

                materials.setStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX, new ItemStack(second.getUpgradeItem(player.level())));
                helper.assertTrue(menu.tryInstallUpgrade(secondId), "Survival installation with the configured material must still succeed");
                helper.assertTrue(menu.getStatus(second).isEnabled(), "Survival installation must immediately enable the upgrade");
                helper.assertTrue(materials.getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX).isEmpty(), "Survival installation must still consume one material");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void materialOverridesPreserveInstallationAndSyncBoundaries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Map<ResourceLocation, ResourceLocation> original = AirtightUpgradeMaterials.createServerSnapshot();
        ResourceLocation magnetId = MagnetUpgrade.INSTANCE.getID();
        ResourceLocation ironId = BuiltInRegistries.ITEM.getKey(Items.IRON_BLOCK);
        ResourceLocation diamondId = BuiltInRegistries.ITEM.getKey(Items.DIAMOND);
        String expectedStartupMaterial = System.getProperty("createcraftedbeginning.test_upgrade_material");
        if (expectedStartupMaterial != null) {
            helper.assertTrue(ResourceLocation.parse(expectedStartupMaterial).equals(original.get(magnetId)), "KubeJS startup overrides must run after upgrades and custom items are registered");
        }

        try {
            AirtightUpgradeMaterials.set(magnetId, diamondId);
            AirtightUpgradeMaterials.set(magnetId, ironId);
            helper.assertTrue(MagnetUpgrade.INSTANCE.getUpgradeItem(level) == Items.IRON_BLOCK, "The last material override must win");
            helper.assertTrue(!MagnetUpgrade.INSTANCE.testUpgradeItem(new ItemStack(Items.HOPPER), level), "The original material must stop matching after replacement");
            helper.assertTrue(!MagnetUpgrade.INSTANCE.testUpgradeItem(new ItemStack(Items.DIAMOND), level), "An earlier override must stop matching");
            helper.assertTrue(!MagnetUpgrade.INSTANCE.testUpgradeItem(ItemStack.EMPTY, level), "Empty stacks must not match");

            assertRejected(helper, magnetId, ResourceLocation.parse("minecraft:air"));
            assertRejected(helper, magnetId, CCBAPI.asResource("missing_upgrade_material"));
            assertRejected(helper, CCBAPI.asResource("missing_upgrade"), ironId);
            assertRejected(helper, HandheldDrillFilterButton.INSTANCE.getID(), ironId);
            helper.assertTrue(MagnetUpgrade.INSTANCE.getUpgradeItem(level) == Items.IRON_BLOCK, "Invalid overrides must retain the last valid material");

            AirtightUpgradeMaterials.set(HarvestOptimizationUpgrade.INSTANCE.getID(), ironId);
            AirtightUpgradeMaterials.set(GogglesUpgrade.INSTANCE.getID(), ironId);
            AirtightUpgradeMaterials.set(VisionUpgrade.INSTANCE.getID(), ironId);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_HANDHELD_DRILL.asItem()));
            AirtightUpgradableMenu drillMenu = (AirtightUpgradableMenu) CCBItems.AIRTIGHT_HANDHELD_DRILL.get().createMenu(1, player.getInventory(), player);
            if (drillMenu == null) {
                throw new NullPointerException("Expected an airtight handheld drill upgrade menu.");
            }

            assertSharedMaterialInstallation(helper, drillMenu, HarvestOptimizationUpgrade.INSTANCE, MagnetUpgrade.INSTANCE);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CCBItems.AIRTIGHT_HELMET.asItem()));
            AirtightUpgradableMenu helmetMenu = (AirtightUpgradableMenu) CCBItems.AIRTIGHT_HELMET.get().createMenu(2, player.getInventory(), player);
            if (helmetMenu == null) {
                throw new NullPointerException("Expected an airtight helmet upgrade menu.");
            }

            assertSharedMaterialInstallation(helper, helmetMenu, GogglesUpgrade.INSTANCE, VisionUpgrade.INSTANCE);

            Map<ResourceLocation, ResourceLocation> clientSnapshot = new HashMap<>(AirtightUpgradeMaterials.createServerSnapshot());
            clientSnapshot.put(magnetId, diamondId);
            AirtightUpgradeMaterialsSyncPacket packet = new AirtightUpgradeMaterialsSyncPacket(clientSnapshot);
            clientSnapshot.clear();
            ByteBuf buffer = Unpooled.buffer();
            try {
                AirtightUpgradeMaterialsSyncPacket.STREAM_CODEC.encode(buffer, packet);
                AirtightUpgradeMaterialsSyncPacket decoded = AirtightUpgradeMaterialsSyncPacket.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.materials().equals(packet.materials()), "Material sync must preserve the complete snapshot");
                AirtightUpgradeMaterials.acceptClientSync(decoded.materials());
            }
            finally {
                buffer.release();
            }

            helper.assertTrue(AirtightUpgradeMaterials.resolveMaterial(MagnetUpgrade.INSTANCE, true) == Items.DIAMOND, "Client material must follow the server snapshot");
            helper.assertTrue(AirtightUpgradeMaterials.resolveMaterial(MagnetUpgrade.INSTANCE, false) == Items.IRON_BLOCK, "Client sync must not overwrite integrated server materials");
            AirtightUpgradeMaterials.acceptClientSync(Map.of(magnetId, ironId));
            helper.assertTrue(AirtightUpgradeMaterials.resolveMaterial(MagnetUpgrade.INSTANCE, true) == Items.IRON_BLOCK, "A new server snapshot must replace the old one");
            AirtightUpgradeMaterials.clearClientMaterials();
            helper.assertTrue(AirtightUpgradeMaterials.resolveMaterial(MagnetUpgrade.INSTANCE, true) == MagnetUpgrade.INSTANCE.getDefaultUpgradeItem(), "Disconnect must clear materials from the previous server");
        }
        finally {
            original.forEach(AirtightUpgradeMaterials::set);
            AirtightUpgradeMaterials.clearClientMaterials();
        }
        helper.succeed();
    }

    private static void assertRejected(GameTestHelper helper, ResourceLocation upgradeId, ResourceLocation itemId) {
        boolean rejected = false;
        try {
            AirtightUpgradeMaterials.set(upgradeId, itemId);
        }
        catch (IllegalArgumentException exception) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Invalid upgrade material assignment must be rejected: " + upgradeId + " -> " + itemId);
    }

    private static void assertSharedMaterialInstallation(GameTestHelper helper, AirtightUpgradableMenu menu, AirtightUpgrade first, AirtightUpgrade second) {
        InventoryHandler inventory = menu.getMenuInventory();
        ResourceLocation firstId = first.getID();
        ResourceLocation secondId = second.getID();
        ItemStack material = new ItemStack(Items.IRON_BLOCK);
        helper.assertTrue(menu.getSlot(Inventory.INVENTORY_SIZE).mayPlace(material), "A shared material must be accepted before installation");
        inventory.setStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX, new ItemStack(Items.DIRT));
        helper.assertTrue(!menu.tryInstallUpgrade(firstId), "Server installation must reject the wrong material");
        helper.assertTrue(inventory.getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX).is(Items.DIRT), "Failed installation must not consume the material");
        inventory.setStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX, material.copy());
        helper.assertTrue(menu.tryInstallUpgrade(firstId), "The first upgrade must install");
        helper.assertTrue(inventory.getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX).isEmpty(), "Installation must consume one material");
        helper.assertTrue(menu.getSlot(Inventory.INVENTORY_SIZE).mayPlace(material), "An installed upgrade must not block another upgrade using the same material");
        inventory.setStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX, material.copy());
        helper.assertTrue(!menu.tryInstallUpgrade(firstId), "An installed upgrade must not install twice");
        helper.assertTrue(menu.tryInstallUpgrade(secondId), "The selected second upgrade must install independently");
        AirtightUpgradeMaterials.set(firstId, BuiltInRegistries.ITEM.getKey(Items.GOLD_BLOCK));
        helper.assertTrue(menu.getStatus(first).isInstalled() && menu.getStatus(second).isInstalled(), "Material changes must preserve installed status");
    }
}
