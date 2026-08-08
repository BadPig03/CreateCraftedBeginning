package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class GasCanisterPackOverrides {
    private static final int LEFT_UP = 1;
    private static final int RIGHT_UP = 2;
    private static final int LEFT_DOWN = 4;
    private static final int RIGHT_DOWN = 8;

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
        if (!rightDown) {
            return flags;
        }

        flags |= RIGHT_DOWN;
        return flags;
    }

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
        public static final ResourceLocation TYPE = CreateCraftedBeginning.asResource("gas_canister_pack_type");

        public static GasCanisterPackType getTypeFromFlags(int flags) {
            return values()[flags & 0b1111];
        }

        @Override
        public String getSerializedName() {
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
}
