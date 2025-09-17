package net.ty.createcraftedbeginning.content.airtightcannon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.mixin.accessor.FallingBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.airtightcannon.AirtightCannonProjectileBlockHitAction;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBAirtightCannonProjectileBlockHitActions {
    static {
        register("place_block_on_ground", PlaceBlockOnGround.CODEC);
    }

    public static void init() {
    }

    @SuppressWarnings("SameParameterValue")
    private static void register(String name, MapCodec<? extends AirtightCannonProjectileBlockHitAction> codec) {
        Registry.register(CCBBuiltInRegistries.POTATO_PROJECTILE_BLOCK_HIT_ACTION, CreateCraftedBeginning.asResource(name), codec);
    }

    public record PlaceBlockOnGround(Holder<Block> block) implements AirtightCannonProjectileBlockHitAction {
        public static final MapCodec<PlaceBlockOnGround> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(BuiltInRegistries.BLOCK.holderByNameCodec().fieldOf("block").forGetter(PlaceBlockOnGround::block)).apply(instance, PlaceBlockOnGround::new));

        @SuppressWarnings("deprecation")
        public PlaceBlockOnGround(@NotNull Block block) {
            this(block.builtInRegistryHolder());
        }

        @Override
        public boolean execute(@NotNull LevelAccessor levelAccessor, ItemStack projectile, BlockHitResult ray) {
            if (levelAccessor.isClientSide()) {
                return true;
            }

            BlockPos hitPos = ray.getBlockPos();
            if (levelAccessor instanceof Level l && !l.isLoaded(hitPos)) {
                return true;
            }
            Direction face = ray.getDirection();
            BlockPos placePos = hitPos.relative(face);
            if (!levelAccessor.getBlockState(placePos).canBeReplaced()) {
                return false;
            }

            if (face == Direction.UP) {
                levelAccessor.setBlock(placePos, block.value().defaultBlockState(), 3);
            } else if (levelAccessor instanceof Level level) {
                double y = ray.getLocation().y - 0.5;
                if (!level.isEmptyBlock(placePos.above())) {
                    y = Math.min(y, placePos.getY());
                }
                if (!level.isEmptyBlock(placePos.below())) {
                    y = Math.max(y, placePos.getY());
                }

                FallingBlockEntity falling = FallingBlockEntityAccessor.create$callInit(level, placePos.getX() + 0.5, y, placePos.getZ() + 0.5, block.value().defaultBlockState());
                falling.time = 1;
                level.addFreshEntity(falling);
            }

            return true;
        }

        @Override
        public MapCodec<? extends AirtightCannonProjectileBlockHitAction> codec() {
            return CODEC;
        }
    }
}
