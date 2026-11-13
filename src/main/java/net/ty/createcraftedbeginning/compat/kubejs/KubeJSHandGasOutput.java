package net.ty.createcraftedbeginning.compat.kubejs;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilter;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterDescription;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterItem;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilters;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class KubeJSHandGasOutput {
    private KubeJSHandGasOutput() {
    }

    public static void append(ServerPlayer player, ItemStack stack) {
        List<Component> messages = buildMessages(player.registryAccess(), stack);
        for (Component message : messages) {
            player.sendSystemMessage(message);
        }
    }

    static List<Component> buildMessages(Provider registries, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof GasFilter filter)) {
            return List.of();
        }

        GasFilterDescription description = filter.describe(GasFilters.normalizeStack(stack));
        if (description == null) {
            return List.of(Component.literal("Gas filter:"), Component.literal("Gas samples cannot be enumerated.").withStyle(ChatFormatting.GRAY));
        }

        boolean configuredFilter = stack.getItem() instanceof GasFilterItem;
        List<GasStack> gases = description.gases();
        if (gases.isEmpty() && !configuredFilter) {
            return List.of();
        }

        List<Component> messages = new ArrayList<>();
        String header = configuredFilter ? "Gas filter:" : "Held gas:";
        messages.add(Component.literal(header));
        if (configuredFilter) {
            String listMode = description.blacklist() ? "Blacklist" : "Whitelist";
            String dataMode = description.respectData() ? "Respect Data" : "Ignore Data";
            messages.add(Component.literal("Filter: " + listMode + ", " + dataMode).withStyle(ChatFormatting.GRAY));
        }

        if (gases.isEmpty()) {
            String emptyMode = description.blacklist() ? "No gas samples; allows all gases." : "No gas samples; blocks all gases.";
            messages.add(Component.literal(emptyMode).withStyle(ChatFormatting.GRAY));
            return List.copyOf(messages);
        }

        RegistryLookup<Gas> gasRegistry = registries.lookupOrThrow(GasRegistries.GAS_REGISTRY_KEY);
        for (GasStack gas : gases) {
            Holder<Gas> gasHolder = gas.getGasHolder();
            String id = gasHolder.unwrapKey().orElseThrow().location().toString();
            messages.add(copy(id, ChatFormatting.GREEN, "Gas ID"));
            DataComponentPatch components = gas.getComponentsPatch();
            if (!components.isEmpty()) {
                String serializedComponents = DataComponentPatch.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), components).getOrThrow().toString();
                messages.add(copy(serializedComponents, ChatFormatting.AQUA, "Gas Components"));
            }

            for (TagKey<Gas> tag : gasHolder.tags().toList()) {
                String tagId = "'#%s'".formatted(tag.location());
                int size = gasRegistry.get(tag).map(HolderSet::size).orElse(0);
                messages.add(copy(tagId, ChatFormatting.YELLOW, "Gas Tag [" + size + " gases]"));
            }
        }
        return List.copyOf(messages);
    }

    private static Component copy(String value, ChatFormatting color, String tooltip) {
        Component text = Component.literal(value).withStyle(color);
        Component hover = Component.literal(tooltip + " (Click to copy)");
        return Component.literal("- ").withStyle(ChatFormatting.GRAY).withStyle(Style.EMPTY.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, value))).withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))).append(text);
    }
}
