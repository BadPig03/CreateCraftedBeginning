package net.ty.createcraftedbeginning.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.ty.createcraftedbeginning.content.breezechamber.BreezeChamberBlockEntity.CoolantType;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class CoolantDataManager {
    public static final Map<ResourceLocation, CoolantData> ITEM_COOLANT_DATA = new HashMap<>();
    public static final Map<ResourceLocation, FluidCoolantData> FLUID_COOLANT_DATA = new HashMap<>();
    public static final Gson GSON = new GsonBuilder().create();

    public static CoolantData getItemCoolantData(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return ITEM_COOLANT_DATA.get(id);
    }

    public static FluidCoolantData getFluidCoolantData(Fluid fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        return FLUID_COOLANT_DATA.get(id);
    }

    public static class DataLoader extends SimplePreparableReloadListener<Object> {
        @Override
        protected @NotNull Object prepare(@NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
            Map<ResourceLocation, CoolantData> itemData = new HashMap<>();
            Map<ResourceLocation, FluidCoolantData> fluidData = new HashMap<>();

            Map<ResourceLocation, Resource> resources = manager.listResources("coolant", id -> id.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
                    for (JsonElement element : array) {
                        JsonObject obj = element.getAsJsonObject();
                        String type = obj.get("type").getAsString();

                        if ("item".equals(type)) {
                            CoolantData data = GSON.fromJson(element, CoolantData.class);
                            ResourceLocation id = ResourceLocation.tryParse(data.id);
                            if (id != null) {
                                itemData.put(id, data);
                            }
                        } else if ("fluid".equals(type)) {
                            FluidCoolantData data = GSON.fromJson(element, FluidCoolantData.class);
                            ResourceLocation id = ResourceLocation.tryParse(data.id);
                            if (id != null) {
                                fluidData.put(id, data);
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            return new Object[]{itemData, fluidData};
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void apply(@NotNull Object preparedData, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
            Object[] data = (Object[]) preparedData;
            ITEM_COOLANT_DATA.clear();
            FLUID_COOLANT_DATA.clear();

            Map<ResourceLocation, CoolantData> itemData = (Map<ResourceLocation, CoolantData>) data[0];
            Map<ResourceLocation, FluidCoolantData> fluidData = (Map<ResourceLocation, FluidCoolantData>) data[1];

            ITEM_COOLANT_DATA.putAll(itemData);
            FLUID_COOLANT_DATA.putAll(fluidData);
        }
    }

    public static class CoolantData {
        public String id;
        public int time;
        public String remaining;
        public String coolant;

        public CoolantType getCoolantType() {
            return "powerful".equalsIgnoreCase(coolant) ? CoolantType.POWERFUL : CoolantType.NORMAL;
        }

        public ResourceLocation getRemainingId() {
            return remaining != null ? ResourceLocation.tryParse(remaining) : null;
        }
    }

    public static class FluidCoolantData {
        public String type;
        public String id;
        public int amount;
        public int time;
        public String coolant;

        public CoolantType getCoolantType() {
            return "powerful".equalsIgnoreCase(coolant) ? CoolantType.POWERFUL : CoolantType.NORMAL;
        }
    }
}
