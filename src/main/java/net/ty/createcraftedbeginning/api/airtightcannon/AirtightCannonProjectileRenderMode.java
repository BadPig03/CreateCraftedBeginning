package net.ty.createcraftedbeginning.api.airtightcannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtightcannon.AirtightCannonProjectileEntity;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;

import java.util.function.Function;

public interface AirtightCannonProjectileRenderMode {
    Codec<AirtightCannonProjectileRenderMode> CODEC = CCBBuiltInRegistries.POTATO_PROJECTILE_RENDER_MODE.byNameCodec().dispatch(AirtightCannonProjectileRenderMode::codec, Function.identity());

    @OnlyIn(Dist.CLIENT)
    void transform(PoseStack ms, AirtightCannonProjectileEntity entity, float pt);

    MapCodec<? extends AirtightCannonProjectileRenderMode> codec();
}
