package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public final class GasCanisterPackContents {
    private static final int MAX_SIZE = 4;
    private static final String COMPOUND_KEY_UUID = "UUID";
    private static final String COMPOUND_KEY_CONTENTS = "Contents";

    public static final GasCanisterPackContents EMPTY = new GasCanisterPackContents(GasCanisterPackUtils.FALLBACK_UUID, NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY));
    public static final Codec<GasCanisterPackContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(UUIDUtil.CODEC.fieldOf("uuid").forGetter(contents -> contents.uuid), ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(contents -> contents.items)).apply(instance, GasCanisterPackContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterPackContents> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, contents -> contents.uuid, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SIZE)), contents -> contents.items, GasCanisterPackContents::new);

    private final NonNullList<ItemStack> items;
    private final UUID uuid;
    private final int hashCode;

    public GasCanisterPackContents(UUID uuid, @NotNull List<ItemStack> items) {
        if (items.size() != MAX_SIZE) {
            throw new IllegalArgumentException("Gas canister pack must have exactly " + MAX_SIZE + " slots");
        }

        this.items = NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < MAX_SIZE; i++) {
            this.items.set(i, items.get(i));
        }
        this.uuid = uuid;
        hashCode = ItemStack.hashStackList(this.items);
    }

    public GasCanisterPackContents(UUID uuid) {
        this(uuid, NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY));
    }

    @Contract("_, _ -> new")
    public static @NotNull GasCanisterPackContents fromItemStackHandler(UUID uuid, @NotNull ItemStackHandler handler) {
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(handler.getSlots(), MAX_SIZE); i++) {
            items.set(i, handler.getStackInSlot(i));
        }
        return new GasCanisterPackContents(uuid, items);
    }

    public static @NotNull GasCanisterPackContents read(@NotNull CompoundTag compoundTag, Provider provider) {
        UUID uuid = compoundTag.getUUID(COMPOUND_KEY_UUID);
        GasCanisterPackContents contents = new GasCanisterPackContents(uuid);
        List<ItemStack> canisters = NBTHelper.readItemList(compoundTag.getList(COMPOUND_KEY_CONTENTS, Tag.TAG_COMPOUND), provider);
        for (int slot = 0; slot < 4; slot++) {
            contents.setStackInSlot(slot, canisters.get(slot).copy());
        }
        return contents;
    }

    @Contract(pure = true)
    public @NotNull CompoundTag write(Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_CONTENTS, NBTHelper.writeItemList(items, provider));
        compoundTag.putUUID(COMPOUND_KEY_UUID, uuid);
        return compoundTag;
    }

    public @NotNull ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return items.get(slot);
    }

    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);
        items.set(slot, stack);
    }

    private static void validateSlotIndex(int slot) {
        if (slot >= 0 && slot < MAX_SIZE) {
            return;
        }

        throw new IllegalArgumentException("Slot " + slot + " out of range [0," + MAX_SIZE + ')');
    }

    public void fillItemStackHandler(@NotNull ItemStackHandler handler) {
        for (int i = 0; i < Math.min(items.size(), handler.getSlots()); i++) {
            handler.setStackInSlot(i, items.get(i));
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof GasCanisterPackContents otherContents && ItemStack.listMatches(items, otherContents.items);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}