package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import com.simibubi.create.content.equipment.armor.TrimmableArmorModelGenerator;
import com.simibubi.create.foundation.mixin.accessor.ItemModelGeneratorsAccessor;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.ItemModelGenerators.TrimModelData;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmorsTrimsModelGenerator extends TrimmableArmorModelGenerator {
    @SuppressWarnings("unchecked")
    public static <T extends ArmorItem> void generate(DataGenContext<Item, T> context, RegistrateItemModelProvider provider) {
        T item = context.get();
        ItemModelBuilder builder = provider.generated(context);
        for (TrimModelData data : ItemModelGeneratorsAccessor.create$getGENERATED_TRIM_MODELS()) {
            String trimId = data.name(item.getMaterial());
            ResourceLocation modelLocation = ModelLocationUtils.getModelLocation(item).withSuffix('_' + trimId + "_trim");
            ItemModelBuilder itemModel = provider.withExistingParent(modelLocation.getPath(), "item/generated").texture("layer0", TextureMapping.getItemTexture(item));
            Map<String, String> textures = (Map<String, String>) TEXTURES_HANDLE.get(itemModel);
            ResourceLocation trimTexture = CreateCraftedBeginning.asResource("trims/items/airtight_" + item.getType().getName() + "_trim_" + trimId);
            textures.put("layer1", trimTexture.toString());
            builder.override().predicate(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, data.itemModelIndex()).model(itemModel).end();
        }
    }
}
