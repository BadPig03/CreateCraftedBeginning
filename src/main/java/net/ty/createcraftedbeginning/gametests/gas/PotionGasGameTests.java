package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.potiongas.PotionGas;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.PotionFractionationRecipes;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PotionGasGameTests {
    private PotionGasGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void fractionationRejectsUnsupportedEffectsAndNormalizesYield(GameTestHelper helper) {
        List<PotionContents> unsupported = List.of(new PotionContents(Potions.AWKWARD), new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1))), new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 0))));
        for (PotionContents contents : unsupported) {
            helper.assertTrue(PotionFractionationRecipes.create(PotionFluid.of(250, contents, BottleType.REGULAR)) == null, "Unsupported potion contents must not produce a fractionation recipe.");
        }

        helper.assertTrue(PotionFractionationRecipes.create(PotionFluid.of(249, new PotionContents(Potions.SWIFTNESS), BottleType.REGULAR)) == null, "A partial bottle must not start a batch.");
        FractionationTowerRecipe regular = PotionFractionationRecipes.create(PotionFluid.of(250, new PotionContents(Potions.SWIFTNESS), BottleType.REGULAR));
        FractionationTowerRecipe extended = PotionFractionationRecipes.create(PotionFluid.of(250, new PotionContents(Potions.LONG_SWIFTNESS), BottleType.LINGERING));
        FractionationTowerRecipe strong = PotionFractionationRecipes.create(PotionFluid.of(250, new PotionContents(Potions.STRONG_SWIFTNESS), BottleType.REGULAR));
        if (regular == null || extended == null || strong == null) {
            throw new NullPointerException("Expected recipes for finite single-effect speed potions.");
        }

        GasStack regularGas = regular.getLayerOutputs().get(1).gas();
        GasStack extendedGas = extended.getLayerOutputs().get(1).gas();
        GasStack strongGas = strong.getLayerOutputs().get(1).gas();
        helper.assertTrue(regular.validate().isEmpty() && extended.validate().isEmpty() && strong.validate().isEmpty(), "Generated fractionation recipes must satisfy tower validation.");
        helper.assertTrue(regularGas.getAmount() == 7200 && extendedGas.getAmount() == 19200 && strongGas.getAmount() == 3600, "Gas yield must reflect source duration without penalizing effect level.");
        helper.assertTrue(GasStack.isSameGasSameComponents(regularGas, extendedGas) && !GasStack.isSameGasSameComponents(regularGas, strongGas), "Normalization must merge duration variants while retaining effect levels.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void instantFractionationHasFixedYieldAcrossLevelsAndBottleTypes(GameTestHelper helper) {
        List<PotionContents> potions = List.of(new PotionContents(Potions.HEALING), new PotionContents(Potions.STRONG_HEALING), new PotionContents(Potions.HARMING), new PotionContents(Potions.STRONG_HARMING), new PotionContents(Optional.empty(), Optional.of(0x123456), List.of(new MobEffectInstance(MobEffects.SATURATION, 0, 2))));
        for (PotionContents contents : potions) {
            MobEffectInstance originalEffect = contents.getAllEffects().iterator().next();
            GasStack previous = GasStack.EMPTY;
            for (BottleType bottleType : BottleType.values()) {
                FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, contents, bottleType));
                if (recipe == null) {
                    throw new NullPointerException("Expected an instant potion fractionation recipe for " + bottleType + '.');
                }

                GasStack gas = recipe.getLayerOutputs().get(1).gas();
                MobEffectInstance effect = PotionGas.findReleaseEffect(gas);
                if (effect == null) {
                    throw new NullPointerException("Expected a supported instant effect in the fractionation product.");
                }

                helper.assertTrue(recipe.validate().isEmpty() && recipe.getProcessingDuration() == 200 && recipe.getRequiredHeight() == 3, "Instant potion fractionation must retain tower processing requirements.");
                helper.assertTrue(gas.getAmount() == 800 && recipe.getLayerOutputs().getFirst().fluid().getAmount() == 250, "Each instant potion batch must produce 800 GU and 250 mB water.");
                helper.assertTrue(effect.getEffect().equals(originalEffect.getEffect()) && effect.getAmplifier() == originalEffect.getAmplifier() && effect.getDuration() == 20, "Instant gas must preserve effect and amplifier with normalized identity duration.");
                helper.assertTrue(PotionGas.getContents(gas).customColor().equals(contents.customColor()), "Instant gas must preserve its custom color.");
                helper.assertTrue(gas.getGasType().getTooltip(gas).isEmpty(), "Supported instant gas must not display an unsupported-effect warning.");
                if (!previous.isEmpty()) {
                    helper.assertTrue(GasStack.isSameGasSameComponents(previous, gas), "Bottle variants must produce mergeable instant gas.");
                }
                previous = gas;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void potionDataSurvivesStorageAndNetwork(GameTestHelper helper) {
        PotionContents contents = new PotionContents(Potions.LONG_SWIFTNESS);
        GasStack original = CCBGases.POTION_GAS.get().createStack(7200, contents);
        RegistryAccess registries = helper.getLevel().registryAccess();
        GasStack restored = GasStack.parseOptional(registries, (CompoundTag) original.saveOptional(registries));
        helper.assertTrue(GasStack.matches(original, restored), "Potion gas must survive NBT serialization.");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.OTHER);
        try {
            GasStack.STREAM_CODEC.encode(buffer, original);
            GasStack decoded = GasStack.STREAM_CODEC.decode(buffer);
            helper.assertTrue(GasStack.matches(original, decoded), "Potion gas must survive network serialization.");
        }
        finally {
            buffer.release();
        }

        GasStack copy = original.copy();
        copy.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
        helper.assertTrue(PotionGas.getContents(original).equals(contents), "Changing a copy must not change the source potion.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void potionInformationSurvivesNormalizationStorageAndFilterSampling(GameTestHelper helper) {
        PotionGas potionGas = CCBGases.POTION_GAS.get();
        GasStack basic = potionGas.createStack(1, new PotionContents(Potions.SWIFTNESS));
        RegistryAccess registries = helper.getLevel().registryAccess();
        for (int amplifier : List.of(1, 6)) {
            PotionContents contents = new PotionContents(Optional.empty(), Optional.of(0x123456), List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, amplifier)));
            GasStack original = potionGas.createStack(1, contents);
            FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(250, contents, BottleType.REGULAR));
            if (recipe == null) {
                throw new NullPointerException("Expected a recipe for display acceptance at amplifier " + amplifier + '.');
            }

            GasStack product = recipe.getLayerOutputs().get(1).gas();
            GasStack restored = GasStack.parseOptional(registries, (CompoundTag) product.saveOptional(registries));
            ItemStack sample = VirtualGasItems.createVirtualItem(restored);
            Component originalName = original.getHoverName();
            Component productName = product.getHoverName();
            Component restoredName = restored.getHoverName();
            int tint = restored.getHint();
            helper.assertTrue(!originalName.equals(basic.getHoverName()), "Potion names must distinguish effect levels, including levels above the translated Roman numerals.");
            helper.assertTrue(originalName.equals(productName) && productName.equals(restoredName) && restoredName.equals(sample.getHoverName()), "Normalization, storage and filter sampling must retain the effect name and level.");
            helper.assertTrue(tint == ARGB32.opaque(0x123456) && sample.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0) == tint, "Custom potion color must survive fractionation, storage and filter sampling.");
            helper.assertTrue(potionGas.getTooltip(restored).isEmpty(), "Supported potion information must retain the concise tooltip without routine release details.");
        }
        helper.assertTrue(potionGas.getTooltip(potionGas.createStack(1, PotionContents.EMPTY)).size() == 1 && potionGas.getTooltip(potionGas.createStack(1, new PotionContents(Potions.TURTLE_MASTER))).size() == 1, "Empty and unsupported direct potion gas must retain one short warning.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void differentPotionsCannotMixAndSamplesKeepIdentity(GameTestHelper helper) {
        PotionGas potionGas = CCBGases.POTION_GAS.get();
        PotionContents speedContents = new PotionContents(Potions.SWIFTNESS);
        GasStack speed = potionGas.createStack(100, speedContents);
        GasStack strength = potionGas.createStack(100, new PotionContents(Potions.STRENGTH));
        GasStack extended = potionGas.createStack(100, new PotionContents(Potions.LONG_SWIFTNESS));
        GasTank tank = new GasTank(10000);
        helper.assertTrue(tank.fill(speed, GasAction.EXECUTE) == 100, "Empty tank must accept potion gas.");
        helper.assertTrue(tank.fill(strength, GasAction.EXECUTE) == 0, "Different effects must not mix.");
        helper.assertTrue(tank.fill(extended, GasAction.EXECUTE) == 0, "Unnormalized duration variants must remain distinct.");
        helper.assertTrue(tank.fill(speed, GasAction.EXECUTE) == 100, "Matching potion gas must merge.");
        ItemStack sample = VirtualGasItems.createVirtualItem(speed);
        int tint = speed.getHint();
        helper.assertTrue(GasStack.isSameGasSameComponents(speed, VirtualGasItems.readGasSample(sample)), "Filter sample must preserve potion contents.");
        helper.assertTrue(sample.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0) == tint, "Filter sample must use potion color.");
        helper.assertTrue(tint == ARGB32.opaque(speedContents.getColor()), "Gas tint must follow potion contents.");
        helper.succeed();
    }
}
