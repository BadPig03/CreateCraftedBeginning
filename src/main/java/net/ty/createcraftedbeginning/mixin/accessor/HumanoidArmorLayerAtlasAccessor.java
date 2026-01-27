package net.ty.createcraftedbeginning.mixin.accessor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAtlasAccessor {
    @Accessor("ARMOR_LOCATION_CACHE")
	static Map<String, ResourceLocation> getArmorLocationCache() {
		throw new RuntimeException();
	}

    @Accessor("innerModel")
    HumanoidModel<?> getInnerModel();

	@Accessor("outerModel")
	HumanoidModel<?> getOuterModel();

    @Accessor("armorTrimAtlas")
    TextureAtlas getArmorTrimAtlas();

    @Invoker("setPartVisibility")
	void callSetPartVisibility(HumanoidModel<?> model, EquipmentSlot slot);
}
