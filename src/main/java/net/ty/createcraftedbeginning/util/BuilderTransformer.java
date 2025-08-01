package net.ty.createcraftedbeginning.util;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.ModelGen;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

@SuppressWarnings("removal")
public class BuilderTransformer {
    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> uncontainable_crate(String type) {
        return b -> b
            .blockstate((c, p) -> {
                ResourceLocation crate = p.modLoc("block/crate_" + type);
                ResourceLocation casing = p.modLoc("block/" + type + "_casing");

                ModelFile model = p.models().withExistingParent("block/crate/" + type + "/single", p.modLoc("block/crate/single")).texture("crate", crate).texture("casing", casing);

                p.getVariantBuilder(c.get()).forAllStates(state -> ConfiguredModel.builder().modelFile(model).build());})
            .item(UncontainableBlockItem::new)
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
            .item()
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
}
