package net.ty.createcraftedbeginning.gametests.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.fanprocessing.ChillingFanProcessingType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChillingRecipeGameTests {
    private ChillingRecipeGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void waterBottleChillingPreservesContainerAndRejectsOtherPotions(GameTestHelper helper) {
        ChillingFanProcessingType chilling = new ChillingFanProcessingType();
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        water.set(DataComponents.CUSTOM_NAME, Component.literal("Chilling test water"));
        water.set(DataComponents.REPAIR_COST, 7);
        helper.assertTrue(chilling.canProcess(water, helper.getLevel()), "Named regular water bottles must remain valid chilling inputs.");

        List<ItemStack> outputs = chilling.process(water, helper.getLevel());
        if (outputs == null) {
            throw new NullPointerException("Expected chilling outputs for a regular water bottle.");
        }

        helper.assertValueEqual(outputs.size(), 2, "Water bottle chilling output stack count");
        helper.assertTrue(outputs.stream().anyMatch(stack -> stack.is(Items.SNOWBALL) && stack.getCount() == 1), "Water bottle chilling must produce one snowball.");
        helper.assertTrue(outputs.stream().anyMatch(stack -> stack.is(Items.GLASS_BOTTLE) && stack.getCount() == 1), "Water bottle chilling must return exactly one glass bottle.");
        List<ItemStack> rejected = List.of(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD), PotionContents.createItemStack(Items.POTION, Potions.POISON), PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER), PotionContents.createItemStack(Items.LINGERING_POTION, Potions.WATER), new ItemStack(Items.POTION), new ItemStack(Items.GLASS_BOTTLE));
        for (ItemStack stack : rejected) {
            helper.assertFalse(chilling.canProcess(stack, helper.getLevel()), "Only regular water bottles may match the water bottle chilling recipe.");
        }
        helper.succeed();
    }
}
