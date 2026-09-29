package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllTags.AllItemTags;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCanisterRegistration {
    private GasCanisterRegistration() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanisterPack() {
        return builder -> builder.properties(properties -> properties.stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanister() {
        return builder -> builder.properties(properties -> properties.stacksTo(1).fireResistant()).tag(Items.ENCHANTABLES, CCBItemTags.GAS_CANISTER_ENCHANTABLE.tag, AllItemTags.PRESSURIZED_AIR_SOURCES.tag, ItemTags.VANISHING_ENCHANTABLE);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> creativeGasCanister() {
        return builder -> builder.properties(properties -> properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }
}
