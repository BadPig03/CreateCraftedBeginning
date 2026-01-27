package net.ty.createcraftedbeginning.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterial.Layer;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class CCBArmorMaterials {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateCraftedBeginning.MOD_ID);
    private static final ResourceLocation AIRTIGHT_LOCATION = CreateCraftedBeginning.asResource("airtight");

    public static final Holder<ArmorMaterial> AIRTIGHT = register("airtight", new int[]{4, 9, 7, 4, 11}, 15, CCBSoundEvents.AIRTIGHT_ARMOR_EQUIP.getMainEventHolder(), 6, 0.25f, Ingredient::of, List.of(new Layer(AIRTIGHT_LOCATION)));

    private static @NotNull Holder<ArmorMaterial> register(String name, int[] defense, int enchantmentValue, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient, List<Layer> layers) {
        EnumMap<Type, Integer> enumMap = Arrays.stream(Type.values()).collect(Collectors.toMap(type -> type, type -> defense[type.ordinal()], (a, b) -> b, () -> new EnumMap<>(Type.class)));
        return MATERIALS.register(name, () -> new ArmorMaterial(enumMap, enchantmentValue, equipSound, repairIngredient, layers, toughness, knockbackResistance));
    }

    @Internal
    public static void register(IEventBus eventBus) {
        MATERIALS.register(eventBus);
    }
}
