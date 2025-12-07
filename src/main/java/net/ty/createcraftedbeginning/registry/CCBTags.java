package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.GasTags;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.registry.CCBTags.NameSpace.COMMON;
import static net.ty.createcraftedbeginning.registry.CCBTags.NameSpace.MOD;

@SuppressWarnings("unused")
public class CCBTags {
    public static @NotNull TagKey<Block> commonBlockTag(String path) {
        return commonTag(BuiltInRegistries.BLOCK, path);
    }

    public static <T> @NotNull TagKey<T> commonTag(Registry<T> registry, String path) {
        return optionalTag(registry, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    public static <T> @NotNull TagKey<T> optionalTag(@NotNull Registry<T> registry, ResourceLocation id) {
        return TagKey.create(registry.key(), id);
    }

    public static @NotNull TagKey<Item> commonItemTag(String path) {
        return commonTag(BuiltInRegistries.ITEM, path);
    }

    public static @NotNull TagKey<Fluid> commonFluidTag(String path) {
        return commonTag(BuiltInRegistries.FLUID, path);
    }

    public static @NotNull TagKey<Gas> commonGasTag(String path) {
        return commonTag(CCBGasRegistries.GAS_REGISTRY, path);
    }

    public static void register() {
        CCBBlockTags.init();
        CCBItemTags.init();
        CCBFluidTags.init();
        CCBGasTags.init();
        CCBEntityFlags.init();
    }

    public enum NameSpace {
        MOD(CreateCraftedBeginning.MOD_ID, false, true),
        COMMON("c");

        public final String id;
        public final boolean optionalDefault;
        public final boolean alwaysDataGenDefault;

        NameSpace(String id) {
            this(id, true, false);
        }

        NameSpace(String id, boolean optionalDefault, boolean alwaysDataGenDefault) {
            this.id = id;
            this.optionalDefault = optionalDefault;
            this.alwaysDataGenDefault = alwaysDataGenDefault;
        }
    }

    public enum CCBBlockTags {
        CRATES,
        AIRTIGHT_COMPONENTS;

        public final TagKey<Block> tag;
        public final boolean alwaysDataGen;

        CCBBlockTags() {
            this(MOD);
        }

        CCBBlockTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBBlockTags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBBlockTags(@NotNull NameSpace namespace, String path, boolean optional, boolean always) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optional ? optionalTag(BuiltInRegistries.BLOCK, id) : BlockTags.create(id);
            alwaysDataGen = always;
        }

        CCBBlockTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        private static void init() {
        }
    }

    public enum CCBItemTags {
        AIRTIGHT_COMPONENTS,
        AIRTIGHT_ARMOR,
        AMETHYST(COMMON, "dusts/amethyst"),
        AMETHYST_CRYSTAL_PLATE(COMMON, "plates/amethyst_crystal"),
        CINDER_ALLOY(COMMON, "ingots/cinder"),
        CINDER_CASING_RAW_MATERIALS,
        CRATES,
        CRYING_OBSIDIAN(COMMON, "dusts/crying_obsidian"),
        ICE_CREAMS,
        ICE_CREAM_WITH_FLAVOR;

        public final TagKey<Item> tag;
        public final boolean alwaysDataGen;

        CCBItemTags() {
            this(MOD);
        }

        CCBItemTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBItemTags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBItemTags(@NotNull NameSpace namespace, String path, boolean optional, boolean always) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optional ? optionalTag(BuiltInRegistries.ITEM, id) : ItemTags.create(id);
            alwaysDataGen = always;
        }

        CCBItemTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        private static void init() {
        }

        public boolean matches(@NotNull ItemStack stack) {
            return stack.is(tag);
        }
    }

    public enum CCBFluidTags {
        ;

        public final TagKey<Fluid> tag;
        public final boolean alwaysDataGen;

        CCBFluidTags() {
            this(MOD);
        }

        CCBFluidTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBFluidTags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBFluidTags(@NotNull NameSpace namespace, String path, boolean optional, boolean always) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optional ? optionalTag(BuiltInRegistries.FLUID, id) : FluidTags.create(id);
            alwaysDataGen = always;
        }

        CCBFluidTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        private static void init() {
        }
    }

    public enum CCBEntityFlags {
        BREEZE_CHAMBER_CAPTURABLE;

        public final TagKey<EntityType<?>> tag;
        public final boolean alwaysDataGen;

        CCBEntityFlags() {
            this(MOD);
        }

        CCBEntityFlags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBEntityFlags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBEntityFlags(@NotNull NameSpace namespace, String path, boolean optional, boolean always) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optional ? optionalTag(BuiltInRegistries.ENTITY_TYPE, id) : TagKey.create(Registries.ENTITY_TYPE, id);
            alwaysDataGen = always;
        }

        CCBEntityFlags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        private static void init() {
        }

        public boolean matches(@NotNull Entity entity) {
            return matches(entity.getType());
        }

        public boolean matches(@NotNull EntityType<?> type) {
            return type.is(tag);
        }
    }

    public enum CCBGasTags {
        NATURAL,
        ULTRAWARM,
        ETHEREAL,
        MOIST,
        SPORE,
        ENERGIZED,
        PRESSURIZED,
        CREATIVE;

        public final TagKey<Gas> tag;
        public final boolean alwaysDataGen;

        CCBGasTags() {
            this(MOD);
        }

        CCBGasTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBGasTags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBGasTags(@NotNull NameSpace namespace, String path, boolean optional, boolean always) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            tag = optional ? optionalTag(CCBGasRegistries.GAS_REGISTRY, id) : GasTags.create(id);
            alwaysDataGen = always;
        }

        CCBGasTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        private static void init() {
        }
    }
}
