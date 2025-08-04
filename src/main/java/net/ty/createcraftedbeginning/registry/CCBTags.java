package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import static net.ty.createcraftedbeginning.registry.CCBTags.NameSpace.COMMON;
import static net.ty.createcraftedbeginning.registry.CCBTags.NameSpace.MOD;

public class CCBTags {
    public static <T> TagKey<T> optionalTag(Registry<T> registry, ResourceLocation id) {
        return TagKey.create(registry.key(), id);
    }

    public static <T> TagKey<T> commonTag(Registry<T> registry, String path) {
        return optionalTag(registry, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    public static TagKey<Block> commonBlockTag(String path) {
        return commonTag(BuiltInRegistries.BLOCK, path);
    }

    public static TagKey<Item> commonItemTag(String path) {
        return commonTag(BuiltInRegistries.ITEM, path);
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
        CRATES;

        public final TagKey<Block> tag;
        public final boolean alwaysDataGen;

        CCBBlockTags() {
            this(MOD);
        }

        CCBBlockTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBBlockTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBBlockTags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBBlockTags(NameSpace namespace, String path, boolean optional, boolean alwaysDataGen) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            if (optional) {
                tag = optionalTag(BuiltInRegistries.BLOCK, id);
            } else {
                tag = BlockTags.create(id);
            }
            this.alwaysDataGen = alwaysDataGen;
        }

        private static void init() {
        }
    }

    public enum CCBItemTags {
        CRATES,
        CINDER_CASING_RAW_MATERIALS,
        CINDER_ALLOY(COMMON, "ingots/cinder");

        public final TagKey<Item> tag;
        public final boolean alwaysDataGen;

        CCBItemTags() {
            this(MOD);
        }

        CCBItemTags(NameSpace namespace) {
            this(namespace, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBItemTags(NameSpace namespace, String path) {
            this(namespace, path, namespace.optionalDefault, namespace.alwaysDataGenDefault);
        }

        CCBItemTags(NameSpace namespace, boolean optional, boolean alwaysDataGen) {
            this(namespace, null, optional, alwaysDataGen);
        }

        CCBItemTags(NameSpace namespace, String path, boolean optional, boolean alwaysDataGen) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace.id, path == null ? Lang.asId(name()) : path);
            if (optional) {
                tag = optionalTag(BuiltInRegistries.ITEM, id);
            } else {
                tag = ItemTags.create(id);
            }
            this.alwaysDataGen = alwaysDataGen;
        }

        private static void init() {
        }
    }

    public static void register() {
        CCBBlockTags.init();
        CCBItemTags.init();
    }
}
