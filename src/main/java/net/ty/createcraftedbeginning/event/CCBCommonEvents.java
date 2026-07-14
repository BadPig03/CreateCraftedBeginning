package net.ty.createcraftedbeginning.event;

import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;
import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.foundation.item.ItemHelper;
import net.createmod.catnip.data.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.HorizontalAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasThresholdCondition;
import net.ty.createcraftedbeginning.api.gas.recipes.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.compat.CCBCompatMods;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressUtils;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleUtils;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu.InventoryHandler;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativegascanister.CreativeGasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.residueoutlet.ResidueOutletBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.crates.andesitecrate.AndesiteCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.brasscrate.BrassCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.cardboardcrate.CardboardCrateBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlockEntity;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class CCBCommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        AirCompressorBlockEntity.registerCapabilities(event);
        AirtightForgingPressBlockEntity.registerCapabilities(event);
        AirtightForgingPressStructuralBlockEntity.registerCapabilities(event);
        AirtightForgingPressStructuralShaftBlockEntity.registerCapabilities(event);
        AirtightHatchBlockEntity.registerCapabilities(event);
        AirtightReactorKettleBlockEntity.registerCapabilities(event);
        AirtightReactorKettleStructuralBlockEntity.registerCapabilities(event);
        AirtightTankBlockEntity.registerCapabilities(event);
        AndesiteCrateBlockEntity.registerCapabilities(event);
        BrassCrateBlockEntity.registerCapabilities(event);
        BreezeChamberBlockEntity.registerCapabilities(event);
        BreezeCoolerBlockEntity.registerCapabilities(event);
        CardboardCrateBlockEntity.registerCapabilities(event);
        CreativeAirtightTankBlockEntity.registerCapabilities(event);
        CreativeGasCanisterBlockEntity.registerCapabilities(event);
        GasCanisterBlockEntity.registerCapabilities(event);
        GasInjectionChamberBlockEntity.registerCapabilities(event);
        GasPackagerBlockEntity.registerCapabilities(event);
        GasRepackagerBlockEntity.registerCapabilities(event);
        HorizontalAirtightTankBlockEntity.registerCapabilities(event);
        PortableGasInterfaceBlockEntity.registerCapabilities(event);
        ResidueOutletBlockEntity.registerCapabilities(event);
        SturdyCrateBlockEntity.registerCapabilities(event);
        TeslaTurbineNozzleBlockEntity.registerCapabilities(event);

        CreativeGasCanisterItem.registerCapabilities(event);
        GasCanisterItem.registerCapabilities(event);
        GasCanisterPackItem.registerCapabilities(event);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }

        AirtightWithGasRecipeTrieFinder.invalidateCaches();
        AirtightReactorKettleUtils.invalidateRecipeCaches();
        AirtightForgingPressUtils.invalidateRecipeCaches();
    }

    @SubscribeEvent
    public static void onDeployerRecipeSearch(DeployerRecipeSearchEvent event) {
        Level level = event.getBlockEntity().getLevel();
        if (level == null) {
            return;
        }

        event.addRecipe(() -> SequencedAssemblyWithGasRecipe.getRecipe(level, event.getInventory(), CCBRecipeTypes.DEPLOYING_WITH_GAS.getType(), DeployerApplicationWithGasRecipe.class), 110);
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        event.modify(CCBItems.AIRTIGHT_HANDHELD_DRILL, builder -> {
            builder.set(CCBDataComponents.AIRTIGHT_UPGRADABLE_INVENTORY, ItemHelper.containerContentsFromHandler(new InventoryHandler(AirtightHandheldDrillMenu.MAX_SLOTS)));
            builder.set(CCBDataComponents.DRILL_MINING_TEMPLATE, AirtightHandheldDrillMiningTemplates.CUBOID);
            builder.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
            builder.set(CCBDataComponents.DRILL_MINING_DIRECTION, Direction.NORTH);
            builder.set(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, new BlockPos(0, 0, 0));
            });
        event.modify(CCBItems.GAS_CANISTER, builder -> {
            builder.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, List.of(GasStack.EMPTY));
            builder.set(CCBDataComponents.CANISTER_CONTAINER_CAPACITIES, List.of(0L));
        });
        event.modify(CCBItems.GAS_CANISTER_PACK, builder -> builder.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, GasCanisterPackType._0000.getFlags()));
        event.modify(CCBItems.GAS_VIRTUAL_ITEM, builder -> builder.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE).set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE));
    }

    @SubscribeEvent
    public static void onRegisterEvent(RegisterEvent event) {
        if (event.getRegistry() != BuiltInRegistries.TRIGGER_TYPES) {
            return;
        }

        if (CCBCompatMods.JEI.isLoaded()) {
            MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK), new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK)));
        }
        Schedule.CONDITION_TYPES.add(3, Pair.of(CreateCraftedBeginning.asResource("gas_threshold"), GasThresholdCondition::new));
    }
}
