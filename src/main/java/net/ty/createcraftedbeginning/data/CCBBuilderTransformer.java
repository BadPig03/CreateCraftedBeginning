package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.ty.createcraftedbeginning.registry.CCBTags;

import java.util.function.Supplier;

import static com.simibubi.create.foundation.data.CreateRegistrate.casingConnectivity;
import static com.simibubi.create.foundation.data.CreateRegistrate.connectedTextures;

@SuppressWarnings("removal")
public class CCBBuilderTransformer {
    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> uncontainable_crate(String type) {
        return b -> b
            .blockstate((c, p) -> {
                ResourceLocation crate = p.modLoc("block/crate_" + type);
                ResourceLocation casing = p.modLoc("block/" + type + "_casing");

                ModelFile model = p.models().withExistingParent("block/crate/" + type + "/single", p.modLoc("block/crate/single")).texture("crate", crate).texture("casing", casing);

                p.getVariantBuilder(c.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());})
            .tag(CCBTags.CCBBlockTags.CRATES.tag)
            .item(UncontainableBlockItem::new)
            .tag(CCBTags.CCBItemTags.CRATES.tag)
            .transform(ModelGen.customItemModel("crate", type, "single"));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
        return b -> b
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> {
                ResourceLocation crate = p.modLoc("block/crate_" + type);
                ResourceLocation casing = p.modLoc("block/" + type + "_casing");

                ModelFile model = p.models().withExistingParent("block/crate/" + type + "/single", p.modLoc("block/crate/single")).texture("crate", crate).texture("casing", casing);

                p.getVariantBuilder(c.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());})
            .tag(CCBTags.CCBBlockTags.CRATES.tag)
            .item()
            .tag(CCBTags.CCBItemTags.CRATES.tag)
            .transform(ModelGen.customItemModel("crate", type, "single"));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> pneumatic_engine() {
        return b -> b
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .transform(ModelGen.customItemModel("pneumatic_engine", "item"));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> photo_stress_bearing() {
        return b -> b
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .transform(ModelGen.customItemModel("photo-stress_bearing", "item"));
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cinder_nozzle() {
        return b -> b
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate((c, p) -> p.simpleBlock(c.getEntry(), AssetLookup.partialBaseModel(c, p)))
            .item()
            .transform(ModelGen.customItemModel("cinder_nozzle", "item"));
    }

    public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> casing(Supplier<CTSpriteShiftEntry> ct) {
        return b -> b.initialProperties(SharedProperties::stone)
            .blockstate((c, p) -> p.simpleBlock(c.get()))
            .onRegister(connectedTextures(() -> new EncasedCTBehaviour(ct.get())))
            .onRegister(casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get())))
            .tag(AllTags.AllBlockTags.CASING.tag)
            .item()
            .properties(Item.Properties::fireResistant)
            .tag(AllTags.AllItemTags.CASING.tag)
            .build();
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cube(String name) {
        return b -> b
            .blockstate((c, p) -> p.simpleBlock(c.get(), p.models().cubeAll(c.getName(), p.modLoc("block/" + name))))
            .item()
            .build();
    }
}
