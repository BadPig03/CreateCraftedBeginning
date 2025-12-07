package net.ty.createcraftedbeginning.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.registries.RegisterEvent.RegisterHelper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class CCBSoundEvents {
    public static final Map<ResourceLocation, SoundEntry> ALL = new HashMap<>();

    public static final SoundEntry DENY = create("deny").playExisting(SoundEvents.NOTE_BLOCK_BASS::value, 1.0f, 0.5f).subtitle("Declining boop").category(SoundSource.PLAYERS).build();
    public static final SoundEntry INJECTING = create("injecting").playExisting(SoundEvents.BREEZE_CHARGE, 0.75f, 1.0f).subtitle("Gas injects").category(SoundSource.BLOCKS).build();
    public static final SoundEntry SHEET_ADDED = create("sheet_added").playExisting(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.75f, 1.0f).subtitle("Airtight Sheet added").category(SoundSource.BLOCKS).build();
    public static final SoundEntry SHEET_REMOVED = create("sheet_removed").playExisting(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.75f, 1.0f).subtitle("Airtight Sheet removed").category(SoundSource.BLOCKS).build();
    public static final SoundEntry ROTOR_ADDED = create("rotor_added").playExisting(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.75f, 1.0f).playExisting(SoundEvents.HEAVY_CORE_PLACE, 0.75f, 1.0f).subtitle("Rotor added").category(SoundSource.BLOCKS).build();
    public static final SoundEntry ROTOR_REMOVED = create("rotor_removed").playExisting(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.75f, 1.0f).playExisting(SoundEvents.HEAVY_CORE_BREAK, 0.75f, 1.0f).subtitle("Rotor removed").category(SoundSource.BLOCKS).build();
    public static final SoundEntry CANISTER_ADDED = create("canister_added").playExisting(SoundEvents.ITEM_FRAME_ADD_ITEM, 0.75f, 1.0f).playExisting(SoundEvents.HEAVY_CORE_PLACE, 0.75f, 1.25f).subtitle("Gas Canister added").category(SoundSource.BLOCKS).build();
    public static final SoundEntry CANISTER_REMOVED = create("canister_removed").playExisting(SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.75f, 1.0f).playExisting(SoundEvents.HEAVY_CORE_BREAK, 0.75f, 1.25f).subtitle("Gas Canister removed").category(SoundSource.BLOCKS).build();
    public static final SoundEntry WIND_CHARGE_LAUNCH = create("wind_charge_launch").playExisting(SoundEvents.WIND_CHARGE_THROW, 1.0f, 2.0f).subtitle("Wind charge launched").category(SoundSource.BLOCKS).build();
    public static final SoundEntry AIRTIGHT_ARMOR_EQUIP = create("airtight_armor_equip").playExisting(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.9f, 1.4f).playExisting(SoundEvents.HEAVY_CORE_HIT, 0.9f, 1.4f).subtitle("Airtight equipments clinks").category(SoundSource.PLAYERS).build();
    public static final SoundEntry AIRTIGHT_JETPACK_LAUNCH = create("airtight_jetpack_launch").playExisting(SoundEvents.WIND_CHARGE_BURST.value(), 1.0f, 1.0f).subtitle("Airtight jetpack launches").category(SoundSource.BLOCKS).build();

    @Contract("_ -> new")
    private static @NotNull SoundEntryBuilder create(String name) {
        return create(CreateCraftedBeginning.asResource(name));
    }

    @Contract("_ -> new")
    public static @NotNull SoundEntryBuilder create(ResourceLocation id) {
        return new SoundEntryBuilder(id);
    }

    public static void register(@NotNull RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> ALL.values().forEach(entry -> entry.register(helper)));
    }

    public static void provideLang(BiConsumer<String, String> consumer) {
        ALL.values().stream().filter(SoundEntry::hasSubtitle).forEach(entry -> consumer.accept(entry.getSubtitleKey(), entry.getSubtitle()));
    }

    @Contract("_ -> new")
    public static @NotNull SoundEntryProvider provider(DataGenerator generator) {
        return new SoundEntryProvider(generator);
    }

    public static void playItemPickup(@NotNull Player player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f, 1.0f + player.level().random.nextFloat());
    }

    public static void prepare() {
        ALL.values().forEach(SoundEntry::prepare);
	}

    public abstract static class SoundEntry {
        protected ResourceLocation id;
        protected String subtitle;
        protected SoundSource category;
        protected int attenuationDistance;

        public SoundEntry(ResourceLocation id, String subtitle, SoundSource category, int attenuationDistance) {
            this.id = id;
            this.subtitle = subtitle;
            this.category = category;
            this.attenuationDistance = attenuationDistance;
        }

        public abstract void prepare();

        public abstract void register(RegisterHelper<SoundEvent> registry);

        public abstract void write(JsonObject json);

        public abstract Holder<SoundEvent> getMainEventHolder();

        public abstract SoundEvent getMainEvent();

        public String getSubtitleKey() {
            return id.getNamespace() + ".subtitle." + id.getPath();
        }

        public ResourceLocation getId() {
            return id;
        }

        public boolean hasSubtitle() {
            return subtitle != null;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void playOnServer(Level world, Vec3i pos) {
            playOnServer(world, pos, 1, 1);
        }

        public void playOnServer(Level world, Vec3i pos, float volume, float pitch) {
            play(world, null, pos, volume, pitch);
        }

        public void play(Level world, Player entity, @NotNull Vec3i pos, float volume, float pitch) {
            play(world, entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch);
        }

        public abstract void play(Level world, Player entity, double x, double y, double z, float volume, float pitch);

        public void play(Level world, Player entity, Vec3i pos) {
            play(world, entity, pos, 1, 1);
        }

        public void playFrom(Entity entity) {
            playFrom(entity, 1, 1);
        }

        public void playFrom(@NotNull Entity entity, float volume, float pitch) {
            if (entity.isSilent()) {
                return;
            }

            play(entity.level(), null, entity.blockPosition(), volume, pitch);
        }

        public void play(Level world, Player entity, @NotNull Vec3 pos, float volume, float pitch) {
            play(world, entity, pos.x(), pos.y(), pos.z(), volume, pitch);
        }

        public void playAt(Level world, @NotNull Vec3i pos, float volume, float pitch, boolean fade) {
            playAt(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, volume, pitch, fade);
        }

        public abstract void playAt(Level world, double x, double y, double z, float volume, float pitch, boolean fade);

        public void playAt(Level world, @NotNull Vec3 pos, float volume, float pitch, boolean fade) {
            playAt(world, pos.x(), pos.y(), pos.z(), volume, pitch, fade);
        }
    }

    public static class SoundEntryProvider implements DataProvider {

        private final PackOutput output;

        public SoundEntryProvider(@NotNull DataGenerator generator) {
            output = generator.getPackOutput();
        }

        @Override
        public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
            return generate(output.getOutputFolder(), cache);
        }

        @Override
        public @NotNull String getName() {
            return "Create Crafted Beginning's Custom Sounds";
        }

        public CompletableFuture<?> generate(Path path, CachedOutput cache) {
            path = path.resolve("assets/createcraftedbeginning");
            JsonObject json = new JsonObject();
            ALL.entrySet().stream().sorted(Entry.comparingByKey()).forEach(entry -> entry.getValue().write(json));
            return DataProvider.saveStable(cache, json, path.resolve("sounds.json"));
        }

    }

    public record ConfiguredSoundEvent(Supplier<SoundEvent> event, float volume, float pitch) {}

    public static class SoundEntryBuilder {
        protected ResourceLocation id;
        protected String subtitle = "unregistered";
        protected SoundSource category = SoundSource.BLOCKS;
        protected List<ConfiguredSoundEvent> wrappedEvents;
        protected List<ResourceLocation> variants;
        protected int attenuationDistance;

        public SoundEntryBuilder(ResourceLocation id) {
            wrappedEvents = new ArrayList<>();
            variants = new ArrayList<>();
            this.id = id;
        }

        public SoundEntryBuilder subtitle(String newSubtitle) {
            subtitle = newSubtitle;
            return this;
        }

        public SoundEntryBuilder attenuationDistance(int distance) {
            attenuationDistance = distance;
            return this;
        }

        public SoundEntryBuilder noSubtitle() {
            subtitle = null;
            return this;
        }

        public SoundEntryBuilder category(SoundSource newCategory) {
            category = newCategory;
            return this;
        }

        public SoundEntryBuilder addVariant(String name) {
            return addVariant(CreateCraftedBeginning.asResource(name));
        }

        public SoundEntryBuilder addVariant(ResourceLocation id) {
            variants.add(id);
            return this;
        }

        public SoundEntryBuilder playExisting(SoundEvent event) {
            return playExisting(event, 1, 1);
        }

        public SoundEntryBuilder playExisting(SoundEvent event, float volume, float pitch) {
            return playExisting(() -> event, volume, pitch);
        }

        public SoundEntryBuilder playExisting(Supplier<SoundEvent> event, float volume, float pitch) {
            wrappedEvents.add(new ConfiguredSoundEvent(event, volume, pitch));
            return this;
        }

        public SoundEntryBuilder playExisting(@NotNull Holder<SoundEvent> event) {
            return playExisting(event::value, 1, 1);
        }

        public SoundEntry build() {
            SoundEntry entry = wrappedEvents.isEmpty() ? new CustomSoundEntry(id, variants, subtitle, category, attenuationDistance) : new WrappedSoundEntry(id, subtitle, wrappedEvents, category, attenuationDistance);
            ALL.put(entry.getId(), entry);
            return entry;
        }
    }

    private static class WrappedSoundEntry extends SoundEntry {
        private final List<ConfiguredSoundEvent> wrappedEvents;
        private final List<CompiledSoundEvent> compiledEvents;

        public WrappedSoundEntry(ResourceLocation id, String subtitle, List<ConfiguredSoundEvent> wrappedEvents, SoundSource category, int attenuationDistance) {
            super(id, subtitle, category, attenuationDistance);
            this.wrappedEvents = wrappedEvents;
            compiledEvents = new ArrayList<>();
        }

        @Override
        public void prepare() {
            for (int i = 0; i < wrappedEvents.size(); i++) {
                ConfiguredSoundEvent wrapped = wrappedEvents.get(i);
                ResourceLocation location = getIdOf(i);
                DeferredHolder<SoundEvent, SoundEvent> event = DeferredHolder.create(Registries.SOUND_EVENT, location);
                compiledEvents.add(new CompiledSoundEvent(event, wrapped.volume(), wrapped.pitch()));
            }
        }

        @Override
        public void register(RegisterHelper<SoundEvent> helper) {
            compiledEvents.stream().map(compiledEvent -> compiledEvent.event().getId()).forEach(location -> helper.register(location, SoundEvent.createVariableRangeEvent(location)));
        }

        @Override
        public void write(JsonObject json) {
            for (int i = 0; i < wrappedEvents.size(); i++) {
                ConfiguredSoundEvent event = wrappedEvents.get(i);
                JsonObject entry = new JsonObject();
                JsonArray list = new JsonArray();
                JsonObject object = new JsonObject();
                object.addProperty("name", event.event().get().getLocation().toString());
                object.addProperty("type", "event");
                if (attenuationDistance != 0) {
                    object.addProperty("attenuation_distance", attenuationDistance);
                }
                list.add(object);
                entry.add("sounds", list);
                if (i == 0 && hasSubtitle()) {
                    entry.addProperty("subtitle", getSubtitleKey());
                }
                json.add(getIdOf(i).getPath(), entry);
            }
        }

        @Override
        public Holder<SoundEvent> getMainEventHolder() {
            return compiledEvents.getFirst().event();
        }

        @Override
        public @NotNull SoundEvent getMainEvent() {
            return compiledEvents.getFirst().event().get();
        }

        @Override
        public void play(Level world, Player entity, double x, double y, double z, float volume, float pitch) {
            compiledEvents.forEach(event -> world.playSound(entity, x, y, z, event.event().get(), category, event.volume() * volume, event.pitch() * pitch));
        }

        @Override
        public void playAt(Level world, double x, double y, double z, float volume, float pitch, boolean fade) {
            compiledEvents.forEach(event -> world.playLocalSound(x, y, z, event.event().get(), category, event.volume() * volume, event.pitch() * pitch, fade));
        }

        @Contract("_ -> new")
        protected @NotNull ResourceLocation getIdOf(int i) {
            return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), i == 0 ? id.getPath() : id.getPath() + "_compounded_" + i);
        }

        private record CompiledSoundEvent(DeferredHolder<SoundEvent, SoundEvent> event, float volume, float pitch) {}
    }

    private static class CustomSoundEntry extends SoundEntry {
        protected List<ResourceLocation> variants;
        protected DeferredHolder<SoundEvent, SoundEvent> event;

        public CustomSoundEntry(ResourceLocation id, List<ResourceLocation> variants, String subtitle, SoundSource category, int attenuationDistance) {
            super(id, subtitle, category, attenuationDistance);
            this.variants = variants;
        }

        @Override
        public void prepare() {
            event = DeferredHolder.create(Registries.SOUND_EVENT, id);
        }

        @Override
        public void register(@NotNull RegisterHelper<SoundEvent> helper) {
            ResourceLocation location = event.getId();
            helper.register(location, SoundEvent.createVariableRangeEvent(location));
        }

        @Override
        public void write(JsonObject json) {
            JsonObject entry = new JsonObject();
            JsonArray list = new JsonArray();
            JsonObject object = new JsonObject();
            object.addProperty("name", id.toString());
            object.addProperty("type", "file");
            if (attenuationDistance != 0) {
                object.addProperty("attenuation_distance", attenuationDistance);
            }
            list.add(object);

            for (ResourceLocation variant : variants) {
                object = new JsonObject();
                object.addProperty("name", variant.toString());
                object.addProperty("type", "file");
                if (attenuationDistance != 0) {
                    object.addProperty("attenuation_distance", attenuationDistance);
                }
                list.add(object);
            }

            entry.add("sounds", list);
            if (hasSubtitle()) {
                entry.addProperty("subtitle", getSubtitleKey());
            }
            json.add(id.getPath(), entry);
        }

        @Override
        public Holder<SoundEvent> getMainEventHolder() {
            return event;
        }

        @Override
        public @NotNull SoundEvent getMainEvent() {
            return event.get();
        }

        @Override
        public void play(@NotNull Level world, Player entity, double x, double y, double z, float volume, float pitch) {
            world.playSound(entity, x, y, z, event.get(), category, volume, pitch);
        }

        @Override
        public void playAt(@NotNull Level world, double x, double y, double z, float volume, float pitch, boolean fade) {
            world.playLocalSound(x, y, z, event.get(), category, volume, pitch, fade);
        }
    }
}
