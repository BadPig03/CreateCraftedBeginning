package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LoadingNetworkBinding(UUID id, String name) {
    public static final int MAX_NAME_LENGTH = 48;
    public static final Codec<LoadingNetworkBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(UUIDUtil.CODEC.fieldOf("id").forGetter(LoadingNetworkBinding::id), Codec.STRING.fieldOf("name").forGetter(LoadingNetworkBinding::name)).apply(instance, LoadingNetworkBinding::new));
    private static final String COMPOUND_KEY_NETWORK = "LoadingNetwork";
    private static final String COMPOUND_KEY_ID = "Id";
    private static final String COMPOUND_KEY_NAME = "Name";

    public static @Nullable LoadingNetworkBinding read(CompoundTag tag) {
        CompoundTag network = tag.getCompound(COMPOUND_KEY_NETWORK);
        if (!network.hasUUID(COMPOUND_KEY_ID)) {
            return null;
        }

        return new LoadingNetworkBinding(network.getUUID(COMPOUND_KEY_ID), network.getString(COMPOUND_KEY_NAME));
    }

    public void write(CompoundTag compoundTag) {
        CompoundTag network = new CompoundTag();
        network.putUUID(COMPOUND_KEY_ID, id);
        network.putString(COMPOUND_KEY_NAME, name);
        compoundTag.put(COMPOUND_KEY_NETWORK, network);
    }
}
