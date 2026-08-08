package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsTrimsModelGenerator;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBItemModelTransformer {
    private CCBItemModelTransformer() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> existing() {
        return builder -> builder.model(AssetLookup.existingItemModel());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> withPartials() {
        return builder -> builder.model(AssetLookup.itemModelWithPartials());
    }

    @Contract(pure = true)
    public static <T extends ArmorItem, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> airtightArmor() {
        return builder -> builder.model(AirtightArmorsTrimsModelGenerator::generate);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasInjectionChamberFilter() {
        return builder -> builder.model((context, provider) -> provider.withExistingParent(context.getName(), provider.modLoc("block/gas_injection_chamber/filter_item")));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> balloon(PackageStyle style) {
        return builder -> builder.model((context, provider) -> provider.withExistingParent(context.getName(), provider.modLoc("item/balloon/balloon_" + style.width() + 'x' + style.height())));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> rareBalloon(PackageStyle style) {
        return builder -> builder.model((context, provider) -> provider.withExistingParent(context.getName(), provider.modLoc("item/balloon/balloon_rare_reverted")).texture("0", provider.modLoc("item/balloon/" + style.type())));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanisterPack() {
        return builder -> builder.model((context, provider) -> {
            ItemModelBuilder itemBuilder = provider.generated(context::get);
            for (GasCanisterPackType type : GasCanisterPackType.values()) {
                int index = type.ordinal();
                itemBuilder.override().predicate(GasCanisterPackType.TYPE, index).model(provider.getBuilder(context.getName() + '_' + index).parent(new UncheckedModelFile("item/generated")).texture("layer0", CCBAPI.asResource("item/gas_canister_pack" + type.getSerializedName()))).end();
            }
        });
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanister() {
        return builder -> builder.model((context, provider) -> provider.withExistingParent(context.getName(), provider.modLoc("block/gas_canister")));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> creativeGasCanister() {
        return builder -> builder.model((context, provider) -> provider.withExistingParent(context.getName(), provider.modLoc("block/creative_gas_canister")));
    }
}
