package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.mojang.serialization.Codec;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GasCanisterPackOverrides {
    private static final int LEFT_UP = 1;
    private static final int RIGHT_UP = 2;
    private static final int LEFT_DOWN = 4;
    private static final int RIGHT_DOWN = 8;

    public enum GasCanisterPackType implements StringRepresentable {
        _0000,
        _0001,
        _0010,
        _0011,
        _0100,
        _0101,
        _0110,
        _0111,
        _1000,
        _1001,
        _1010,
        _1011,
        _1100,
        _1101,
        _1110,
        _1111;

        public static final Codec<GasCanisterPackType> CODEC = StringRepresentable.fromValues(GasCanisterPackType::values);
        public static final StreamCodec<ByteBuf, GasCanisterPackType> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(GasCanisterPackType.class);
        private static final ResourceLocation TYPE = CreateCraftedBeginning.asResource("gas_canister_pack_type");

        public static GasCanisterPackType getTypeFromFlags(int flags) {
            return values()[flags & 0b1111];
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }

        public int getFlags() {
            return ordinal();
        }

        public boolean hasLeftUp() {
            return (getFlags() & LEFT_UP) != 0;
        }

        public boolean hasRightUp() {
            return (getFlags() & RIGHT_UP) != 0;
        }

        public boolean hasLeftDown() {
            return (getFlags() & LEFT_DOWN) != 0;
        }

        public boolean hasRightDown() {
            return (getFlags() & RIGHT_DOWN) != 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerModelOverridesClient(GasCanisterPackItem item) {
        ItemProperties.register(item, GasCanisterPackType.TYPE, (stack, level, entity, seed) -> GasCanisterPackType.getTypeFromFlags(stack.getOrDefault(CCBDataComponents.GAS_CANISTER_PACK_FLAGS, 0)).ordinal());
    }

    public static void addOverrideModels(@NotNull DataGenContext<Item, GasCanisterPackItem> context, @NotNull RegistrateItemModelProvider provider) {
        ItemModelBuilder builder = provider.generated(context::get);
        for (GasCanisterPackType type : GasCanisterPackType.values()) {
            int i = type.ordinal();
            builder.override().predicate(GasCanisterPackType.TYPE, i).model(provider.getBuilder(context.getName() + '_' + i).parent(new UncheckedModelFile("item/generated")).texture("layer0", CreateCraftedBeginning.asResource("item/gas_canister_pack" + type.getSerializedName()))).end();
        }
    }

    public static int calculateFlags(boolean leftUp, boolean rightUp, boolean leftDown, boolean rightDown) {
        int flags = 0;
        if (leftUp) {
            flags |= LEFT_UP;
        }
        if (rightUp) {
            flags |= RIGHT_UP;
        }
        if (leftDown) {
            flags |= LEFT_DOWN;
        }
        if (rightDown) {
            flags |= RIGHT_DOWN;
        }
        return flags;
    }
}
