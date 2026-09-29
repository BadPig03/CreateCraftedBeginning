package net.ty.createcraftedbeginning.registry.registrate;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightEquipmentRegistration {
    private AirtightEquipmentRegistration() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> incompleteAirtightEquipment() {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.EPIC).fireResistant());
    }

    @SafeVarargs
    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> airtightEquipment(TagKey<Item>... enchantmentTags) {
        return builder -> {
            ItemBuilder<T, P> result = builder.properties(properties -> properties.rarity(Rarity.EPIC).fireResistant().stacksTo(1));
            if (enchantmentTags.length == 0) {
                return result;
            }

            return result.tag(Items.ENCHANTABLES).tag(enchantmentTags);
        };
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> airtightArmor(TagKey<Item> armorEnchantmentTag) {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).tag(CCBItemTags.AIRTIGHT_ARMOR.tag, Items.ENCHANTABLES, armorEnchantmentTag, ItemTags.VANISHING_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE, ItemTags.TRIMMABLE_ARMOR);
    }
}
