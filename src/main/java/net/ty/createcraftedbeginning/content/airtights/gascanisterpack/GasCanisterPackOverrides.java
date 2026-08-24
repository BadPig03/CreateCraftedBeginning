package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCanisterPackOverrides {
    private static final int LEFT_UP = 1;
    private static final int RIGHT_UP = 2;
    private static final int LEFT_DOWN = 4;
    private static final int RIGHT_DOWN = 8;

    private GasCanisterPackOverrides() {
    }

    static int calculateFlags(boolean leftUp, boolean rightUp, boolean leftDown, boolean rightDown) {
        int occupancyFlags = 0;
        if (leftUp) {
            occupancyFlags |= LEFT_UP;
        }
        if (rightUp) {
            occupancyFlags |= RIGHT_UP;
        }
        if (leftDown) {
            occupancyFlags |= LEFT_DOWN;
        }
        if (!rightDown) {
            return occupancyFlags;
        }

        occupancyFlags |= RIGHT_DOWN;
        return occupancyFlags;
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

        private static final Codec<GasCanisterPackType> CODEC = StringRepresentable.fromValues(GasCanisterPackType::values);
        private static final StreamCodec<ByteBuf, GasCanisterPackType> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(GasCanisterPackType.class);
        public static final ResourceLocation TYPE = CCBAPI.asResource("gas_canister_pack_type");

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
    }
}
