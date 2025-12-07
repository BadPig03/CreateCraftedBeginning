package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBArmorMaterials;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AirtightBaseArmorItem extends ArmorItem {
    protected final ResourceLocation textureLoc = CreateCraftedBeginning.asResource("airtight");

    public AirtightBaseArmorItem(Type type, @NotNull Properties properties) {
		super(CCBArmorMaterials.AIRTIGHT, type, properties.stacksTo(1));
	}

	@Override
	public @Nullable ResourceLocation getArmorTexture(@NotNull ItemStack stack, @NotNull Entity entity, @NotNull EquipmentSlot slot, @NotNull Layer layer, boolean innerModel) {
		int materialLayer = slot == EquipmentSlot.LEGS ? 2 : 1;
		return ResourceLocation.parse(String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d.png", textureLoc.getNamespace(), textureLoc.getPath(), materialLayer));
	}
}
