package net.ty.createcraftedbeginning.ponder.scenes.gaspipes;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilters.GasFilterData;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlockEntity;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SmartAirtightPipeScenes {
    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);

        scene.title("smart_airtight_pipe", "Filtering Gases with Smart Airtight Pipes");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();

        BlockPos encasedPipePos = util.grid().at(1, 1, 1);
        BlockPos secondPipePos = encasedPipePos.east();
        BlockPos smartPos = secondPipePos.east();
        BlockPos firstPipePos = smartPos.east();
        BlockPos firstTankPos = firstPipePos.east();
        BlockPos thirdPipePos = encasedPipePos.south();
        BlockPos middleSouthPipePos = thirdPipePos.south();
        BlockPos fourthPipePos = middleSouthPipePos.south();
        BlockPos secondTankPos = fourthPipePos.south();

        Selection firstTankSelection = util.select().fromTo(firstTankPos, firstTankPos.above());
        Selection secondTankSelection = util.select().fromTo(secondTankPos, secondTankPos.above());
        Selection southPipeSelection = util.select().fromTo(thirdPipePos, fourthPipePos);
        Selection smartSelection = util.select().fromTo(firstPipePos, secondPipePos);
        Selection encasedSelection = util.select().position(encasedPipePos);
        Selection smartSingleSelection = util.select().position(smartPos);

        Vec3 smartVec = util.vector().centerOf(smartPos);
        Vec3 blockedStartVec = util.vector().blockSurface(firstPipePos, Direction.EAST);

        AABB unrestrictedArea = new AABB(smartVec, smartVec);
        AABB matchingArea = new AABB(smartVec, smartVec);
        AABB blockedArea = new AABB(blockedStartVec, blockedStartVec);

        Object unrestrictedObject = new Object();
        Object matchingObject = new Object();
        Object blockedObject = new Object();

        ItemStack naturalAirCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        naturalAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.NATURAL_AIR.get(), 1));

        ItemStack ultrawarmAirCanister = new ItemStack(CCBItems.GAS_CANISTER.asItem());
        ultrawarmAirCanister.set(CCBDataComponents.CANISTER_CONTAINER_CONTENTS, new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1));

        ItemStack configuredGasFilter = new ItemStack(CCBItems.GAS_FILTER.asItem());
        configuredGasFilter.set(CCBDataComponents.GAS_FILTER_DATA, new GasFilterData(false, false, List.of(new GasStack(CCBGases.NATURAL_AIR.get(), 1), new GasStack(CCBGases.ULTRAWARM_AIR.get(), 1))));

        ItemStack configuredGasCanisterPack = new ItemStack(CCBItems.GAS_CANISTER_PACK.asItem());
        configuredGasCanisterPack.set(CCBDataComponents.GAS_CANISTER_PACK_CONTENTS, ItemContainerContents.fromItems(List.of(naturalAirCanister.copy(), ultrawarmAirCanister.copy())));
        configuredGasCanisterPack.set(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, GasCanisterPackType._0011.getFlags());

        scene.idle(20);
        scene.world().showSection(firstTankSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(smartSelection, Direction.SOUTH);

        scene.idle(3);
        scene.world().showSection(encasedSelection, Direction.DOWN);

        scene.idle(3);
        scene.world().showSection(southPipeSelection, Direction.EAST);

        scene.idle(3);
        scene.world().showSection(secondTankSelection, Direction.EAST);

        scene.idle(20);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, ItemStack.EMPTY);
        scene.overlay().showFilterSlotInput(util.vector().topOf(smartPos), Direction.UP, 66);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, unrestrictedObject, unrestrictedArea, 3);

        scene.idle(3);
        unrestrictedArea = unrestrictedArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, unrestrictedObject, unrestrictedArea, 3);

        scene.idle(3);
        unrestrictedArea = unrestrictedArea.inflate(1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, unrestrictedObject, unrestrictedArea, 60);
        scene.overlay().showText(60).text("Without a filter, Smart Airtight Pipes allow any gas to pass").colored(PonderPalette.GREEN).pointAt(smartVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("A filled Gas Canister can specify which gas is allowed through").colored(PonderPalette.BLUE).pointAt(smartVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(smartPos), Pointing.DOWN, 60).rightClick().withItem(naturalAirCanister.copy());

        scene.idle(7);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, naturalAirCanister.copy());

        scene.idle(67);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, matchingObject, matchingArea, 3);

        scene.idle(3);
        matchingArea = matchingArea.inflate(0.5, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, matchingObject, matchingArea, 3);

        scene.idle(3);
        matchingArea = matchingArea.inflate(1, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.GREEN, matchingObject, matchingArea, 60);
        scene.overlay().showText(60).text("Matching gas can pass through the pipe from either side").colored(PonderPalette.GREEN).pointAt(smartVec).placeNearTarget().attachKeyFrame();

        scene.idle(80);
        scene.overlay().showText(60).text("Changing the filter blocks gases that no longer match").colored(PonderPalette.RED).pointAt(smartVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(smartPos), Pointing.DOWN, 60).rightClick().withItem(ultrawarmAirCanister.copy());

        scene.idle(7);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, ultrawarmAirCanister.copy());
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedObject, blockedArea, 3);

        scene.idle(3);
        blockedArea = blockedArea.inflate(0, 0.3125, 0.3125);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedObject, blockedArea, 3);

        scene.idle(3);
        blockedArea = blockedArea.expandTowards(-1.5, 0, 0);
        scene.overlay().chaseBoundingBoxOutline(PonderPalette.RED, blockedObject, blockedArea, 60);

        scene.idle(67);
        scene.overlay().showText(60).text("Gas Filters can use whitelist or blacklist rules for multiple gases").colored(PonderPalette.BLUE).pointAt(smartVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(smartPos), Pointing.DOWN, 60).rightClick().withItem(configuredGasFilter.copy());

        scene.idle(7);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, configuredGasFilter.copy());

        scene.idle(73);
        scene.overlay().showText(60).text("Gas Canister Packs can filter for any gas stored inside").colored(PonderPalette.BLUE).pointAt(smartVec).placeNearTarget().attachKeyFrame();
        scene.overlay().showControls(util.vector().topOf(smartPos), Pointing.DOWN, 60).rightClick().withItem(configuredGasCanisterPack.copy());

        scene.idle(7);
        scene.world().setFilterData(smartSingleSelection, SmartAirtightPipeBlockEntity.class, configuredGasCanisterPack.copy());

        scene.idle(53);
        scene.markAsFinished();
    }
}
