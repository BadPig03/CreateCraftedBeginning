package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBArmorMaterials;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightBaseArmorItem extends ArmorItem {
    private static final ResourceLocation OUTER_TEXTURE = CreateCraftedBeginning.asResource("textures/models/armor/airtight_layer_1.png");
    private static final ResourceLocation INNER_TEXTURE = CreateCraftedBeginning.asResource("textures/models/armor/airtight_layer_2.png");

    public AirtightBaseArmorItem(Type type, Properties properties) {
        super(CCBArmorMaterials.AIRTIGHT, type, properties.stacksTo(1));
    }

    @Override
    public @Nullable ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, Layer layer, boolean innerModel) {
        return slot == EquipmentSlot.LEGS ? INNER_TEXTURE : OUTER_TEXTURE;
    }
}