package net.ty.createcraftedbeginning.api.airtightcannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;

import java.util.function.Function;

public interface AirtightCannonProjectileBlockHitAction {
    Codec<AirtightCannonProjectileBlockHitAction> CODEC = CCBBuiltInRegistries.POTATO_PROJECTILE_BLOCK_HIT_ACTION.byNameCodec().dispatch(AirtightCannonProjectileBlockHitAction::codec, Function.identity());

    boolean execute(LevelAccessor level, ItemStack projectile, BlockHitResult ray);

    MapCodec<? extends AirtightCannonProjectileBlockHitAction> codec();
}