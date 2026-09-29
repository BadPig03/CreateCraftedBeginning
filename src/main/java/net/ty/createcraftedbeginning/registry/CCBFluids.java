package net.ty.createcraftedbeginning.registry;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Source;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.fluids.amethystsuspension.AmethystSuspensionBucketItem;
import net.ty.createcraftedbeginning.content.fluids.amethystsuspension.AmethystSuspensionVirtualFluid;
import net.ty.createcraftedbeginning.content.fluids.brimstone.BrimstoneFluidBlock;
import net.ty.createcraftedbeginning.content.fluids.brimstone.BrimstoneFluidType;
import net.ty.createcraftedbeginning.content.fluids.slush.SlushVirtualFluid;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.CCBCreativeTabSection;
import net.ty.createcraftedbeginning.registry.registrate.CCBCreativeSectionTracker;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBFluids {
    private static final CreateRegistrate CCB_REGISTRATE = CCBRegistrateProvider.get();
    private static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(Keys.FLUID_TYPES, CCBAPI.MOD_ID);

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.OPTICAL_POWER);
    }

    public static final FluidEntry<AmethystSuspensionVirtualFluid> AMETHYST_SUSPENSION = CCB_REGISTRATE.virtualFluid("amethyst_suspension", CCBAPI.asResource("fluid/amethyst_suspension"), CCBAPI.asResource("fluid/amethyst_suspension"), CreateRegistrate::defaultFluidType, AmethystSuspensionVirtualFluid::createSource, AmethystSuspensionVirtualFluid::createFlowing).lang("Amethyst Suspension").bucket(AmethystSuspensionBucketItem::new).build().register();

    static {
        CCBCreativeSectionTracker.set(CCB_REGISTRATE, CCBCreativeTabSection.DECORATIONS);
    }

    public static final FluidEntry<Flowing> BRIMSTONE = CCB_REGISTRATE.standardFluid("brimstone", BrimstoneFluidType.create(0x831812, () -> 0.03125F)).lang("Brimstone").properties(properties -> properties.density(2000).temperature(3000).viscosity(6000).motionScale(0.05).lightLevel(12).canPushEntity(false).canSwim(false).canDrown(false).pathType(PathType.DANGER_OTHER).adjacentPathType(null).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA).supportsBoating(false)).fluidProperties(properties -> properties.levelDecreasePerBlock(3).slopeFindDistance(2).explosionResistance(100).tickRate(40)).source(Source::new).block(BrimstoneFluidBlock::new).properties(properties -> properties.mapColor(MapColor.COLOR_RED)).lang("Brimstone").build().bucket().build().register();

    public static final FluidEntry<SlushVirtualFluid> SLUSH = CCB_REGISTRATE.virtualFluid("slush", CCBAPI.asResource("fluid/slush"), CCBAPI.asResource("fluid/slush"), CreateRegistrate::defaultFluidType, SlushVirtualFluid::createSource, SlushVirtualFluid::createFlowing).lang("Slush").tag(CCBTags.commonFluidTag("snow")).register();

    public static void registerFluidInteractions() {
        FluidType brimstone = BRIMSTONE.get().getFluidType();
        FluidInteractionRegistry.addInteraction(brimstone, new InteractionInformation(NeoForgeMod.WATER_TYPE.value(), fluidState -> Blocks.NETHERRACK.defaultBlockState()));
        FluidInteractionRegistry.addInteraction(brimstone, new InteractionInformation(NeoForgeMod.LAVA_TYPE.value(), fluidState -> Blocks.MAGMA_BLOCK.defaultBlockState()));
        FluidInteractionRegistry.addInteraction(brimstone, new InteractionInformation(AllFluids.HONEY.getType(), fluidState -> AllPaletteStoneTypes.OCHRUM.getBaseBlock().get().defaultBlockState()));
        FluidInteractionRegistry.addInteraction(brimstone, new InteractionInformation(AllFluids.CHOCOLATE.getType(), fluidState -> AllPaletteStoneTypes.CRIMSITE.getBaseBlock().get().defaultBlockState()));
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
