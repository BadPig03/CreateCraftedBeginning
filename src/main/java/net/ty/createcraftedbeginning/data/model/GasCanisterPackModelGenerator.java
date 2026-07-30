package net.ty.createcraftedbeginning.data.model;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackItem;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackOverrides.GasCanisterPackType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasCanisterPackModelGenerator {
    private GasCanisterPackModelGenerator() {
    }

    public static void addOverrideModels(DataGenContext<Item, GasCanisterPackItem> context, RegistrateItemModelProvider provider) {
        ItemModelBuilder builder = provider.generated(context::get);
        for (GasCanisterPackType type : GasCanisterPackType.values()) {
            int index = type.ordinal();
            builder.override().predicate(GasCanisterPackType.TYPE, index).model(provider.getBuilder(context.getName() + '_' + index).parent(new UncheckedModelFile("item/generated")).texture("layer0", CreateCraftedBeginning.asResource("item/gas_canister_pack" + type.getSerializedName()))).end();
        }
    }
}
