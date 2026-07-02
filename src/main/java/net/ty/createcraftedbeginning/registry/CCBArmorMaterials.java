package net.ty.createcraftedbeginning.registry;

import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBArmorMaterials {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, CreateCraftedBeginning.MOD_ID);
    private static final ResourceLocation AIRTIGHT_LOCATION = CreateCraftedBeginning.asResource("airtight");

    public static final Holder<ArmorMaterial> AIRTIGHT = register(new int[]{4, 9, 7, 4, 11}, CCBSoundEvents.AIRTIGHT_ARMOR_EQUIP.getMainEventHolder(), Ingredient::of, List.of(new Layer(AIRTIGHT_LOCATION)));

    private static Holder<ArmorMaterial> register(int[] defense, Holder<SoundEvent> equipSound, Supplier<Ingredient> repairIngredient, List<Layer> layers) {
        EnumMap<Type, Integer> enumMap = Arrays.stream(Type.values()).collect(Collectors.toMap(type -> type, type -> defense[type.ordinal()], (a, b) -> b, () -> new EnumMap<>(Type.class)));
        return MATERIALS.register("airtight", () -> new ArmorMaterial(enumMap, 15, equipSound, repairIngredient, layers, (float) 6, (float) 0.25));
    }

    @Internal
    public static void register(IEventBus eventBus) {
        MATERIALS.register(eventBus);
    }
}
