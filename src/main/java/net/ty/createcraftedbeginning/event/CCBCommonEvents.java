package net.ty.createcraftedbeginning.event;

import com.simibubi.create.content.kinetics.deployer.DeployerRecipeSearchEvent;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.recipes.DeployerApplicationWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu.DrillItemHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.content.airtights.airtighthatch.AirtightHatchBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUtils;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
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
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class CCBCommonEvents {
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        AirCompressorBlockEntity.registerCapabilities(event);
        AirtightTankBlockEntity.registerCapabilities(event);
        AirtightHatchBlockEntity.registerCapabilities(event);
        AndesiteCrateBlockEntity.registerCapabilities(event);
        BrassCrateBlockEntity.registerCapabilities(event);
        BreezeChamberBlockEntity.registerCapabilities(event);
        BreezeCoolerBlockEntity.registerCapabilities(event);
        CardboardCrateBlockEntity.registerCapabilities(event);
        CreativeAirtightTankBlockEntity.registerCapabilities(event);
        GasCanisterBlockEntity.registerCapabilities(event);
        GasInjectionChamberBlockEntity.registerCapabilities(event);
        PortableGasInterfaceBlockEntity.registerCapabilities(event);
        ResidueOutletBlockEntity.registerCapabilities(event);
        SturdyCrateBlockEntity.registerCapabilities(event);
        TeslaTurbineNozzleBlockEntity.registerCapabilities(event);
    }

    @SubscribeEvent
    public static void onLevelLoad(@NotNull Load event) {
        LevelAccessor level = event.getLevel();
        CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.onLevelLoad(level);
    }

    @SubscribeEvent
    public static void onDeployerRecipeSearch(@NotNull DeployerRecipeSearchEvent event) {
        Level level = event.getBlockEntity().getLevel();
        if (level == null) {
            return;
        }

        event.addRecipe(() -> SequencedAssemblyWithGasRecipe.getRecipe(level, event.getInventory(), CCBRecipeTypes.DEPLOYING_WITH_GAS.getType(), DeployerApplicationWithGasRecipe.class), 110);
    }

    @SubscribeEvent
    public static void onModifyDefaultComponents(@NotNull ModifyDefaultComponentsEvent event) {
        event.modify(CCBItems.AIRTIGHT_HANDHELD_DRILL, builder -> {
            builder.set(CCBDataComponents.DRILL_OPTION_FLAGS, AirtightHandheldDrillMenu.getDefaultFlags());
            builder.set(CCBDataComponents.DRILL_MINING_TEMPLATE, AirtightHandheldDrillMiningTemplates.CUBOID);
            builder.set(CCBDataComponents.DRILL_MINING_SIZE, new BlockPos(1, 1, 1));
            builder.set(CCBDataComponents.DRILL_MINING_DIRECTION, Direction.NORTH);
            builder.set(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, new BlockPos(0, 0, 0));
            builder.set(CCBDataComponents.DRILL_INVENTORY, ItemHelper.containerContentsFromHandler(new DrillItemHandler()));
        });
        event.modify(CCBItems.GAS_CANISTER, builder -> builder.set(CCBDataComponents.CANISTER_CONTENT, GasStack.EMPTY));
        event.modify(CCBItems.GAS_CANISTER_PACK, builder -> {
            builder.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, GasCanisterPackType._0000.getFlags());
            builder.set(CCBDataComponents.GAS_CANISTER_PACK_UUID, GasCanisterPackUtils.FALLBACK_UUID);
        });
    }
}
