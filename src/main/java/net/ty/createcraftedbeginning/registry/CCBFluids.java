package net.ty.createcraftedbeginning.registry;

import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Source;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.amethystcrystals.AmethystSuspensionBucketItem;
import net.ty.createcraftedbeginning.content.brimstone.BrimstoneFluidBlock;
import net.ty.createcraftedbeginning.content.brimstone.BrimstoneFluidType;
import net.ty.createcraftedbeginning.content.fluids.AmethystSuspensionVirtualFluid;
import net.ty.createcraftedbeginning.content.fluids.SlushVirtualFluid;
import net.ty.createcraftedbeginning.data.CCBRegistrate;

@SuppressWarnings("unused")
public class CCBFluids {
    private static final CCBRegistrate CCB_REGISTRATE = CreateCraftedBeginning.registrate().setCreativeTab(CCBCreativeTabs.BASE_CREATIVE_TAB);

    public static final FluidEntry<AmethystSuspensionVirtualFluid> AMETHYST_SUSPENSION = CCB_REGISTRATE.amethyst_suspension_fluid("amethyst_suspension").lang("Amethyst Suspension").bucket(AmethystSuspensionBucketItem::new).build().register();

    public static final FluidEntry<Flowing> BRIMSTONE = CCB_REGISTRATE.standardFluid("brimstone", BrimstoneFluidType.create(0x831812, () -> 0.03125f)).lang("Brimstone").properties(p -> p.density(2000).temperature(3000).viscosity(6000).motionScale(0.05).lightLevel(12).canPushEntity(false).canSwim(false).canDrown(false).pathType(PathType.DANGER_OTHER).adjacentPathType(null).sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA).sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA).supportsBoating(false)).fluidProperties(p -> p.levelDecreasePerBlock(3).slopeFindDistance(2).explosionResistance(100.0f).tickRate(40)).source(Source::new).block(BrimstoneFluidBlock::new).properties(p -> p.mapColor(MapColor.COLOR_RED)).lang("Brimstone").build().bucket().build().register();

    public static final FluidEntry<SlushVirtualFluid> SLUSH = CCB_REGISTRATE.slush_fluid("slush").lang("Slush").tag(CCBTags.commonFluidTag("snow")).register();

    public static void register() {
    }
}
