package net.ty.createcraftedbeginning.api.airtightcannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;

import java.util.function.Function;

public interface AirtightCannonProjectileEntityHitAction {
    Codec<AirtightCannonProjectileEntityHitAction> CODEC = CCBBuiltInRegistries.POTATO_PROJECTILE_ENTITY_HIT_ACTION.byNameCodec().dispatch(AirtightCannonProjectileEntityHitAction::codec, Function.identity());

    boolean execute(ItemStack projectile, EntityHitResult ray, Type type);

    MapCodec<? extends AirtightCannonProjectileEntityHitAction> codec();

    enum Type {
        PRE_HIT,
        ON_HIT
    }
}
