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
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmorsTrimsModelGenerator extends TrimmableArmorModelGenerator {
    private AirtightArmorsTrimsModelGenerator() {
    }

    public static <T extends ArmorItem> void generate(DataGenContext<Item, T> context, RegistrateItemModelProvider provider) {
        T armorItem = context.get();
        ItemModelBuilder baseModel = provider.generated(context);
        for (TrimModelData trimData : ItemModelGeneratorsAccessor.create$getGENERATED_TRIM_MODELS()) {
            String trimId = trimData.name(armorItem.getMaterial());
            ResourceLocation modelLocation = ModelLocationUtils.getModelLocation(armorItem).withSuffix('_' + trimId + "_trim");
            ItemModelBuilder trimModel = provider.withExistingParent(modelLocation.getPath(), "item/generated").texture("layer0", TextureMapping.getItemTexture(armorItem));
            @SuppressWarnings("unchecked") Map<String, String> trimTextures = (Map<String, String>) TEXTURES_HANDLE.get(trimModel);
            ResourceLocation trimTexture = CCBAPI.asResource("trims/items/airtight_" + armorItem.getType().getName() + "_trim_" + trimId);
            trimTextures.put("layer1", trimTexture.toString());
            baseModel.override().predicate(ItemModelGenerators.TRIM_TYPE_PREDICATE_ID, trimData.itemModelIndex()).model(trimModel).end();
        }
    }
}
