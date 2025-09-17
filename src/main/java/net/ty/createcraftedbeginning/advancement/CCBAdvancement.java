package net.ty.createcraftedbeginning.advancement;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class CCBAdvancement {
    static final ResourceLocation BACKGROUND = CreateCraftedBeginning.asResource("textures/gui/advancements.png");
    static final String LANG = "advancement." + CreateCraftedBeginning.MOD_ID + ".";

    private final Advancement.Builder mcBuilder = Advancement.Builder.advancement();
    private final Builder ccbBuilder = new CCBAdvancement.Builder();
    AdvancementHolder dataGenResult;
    private SimpleCCBTrigger builtinTrigger;
    private CCBAdvancement parent;
    private final String id;
    private String title;
    private String description;

    public CCBAdvancement(String id, @NotNull UnaryOperator<Builder> b) {
        this.id = id;

        b.apply(ccbBuilder);

        if (!ccbBuilder.externalTrigger) {
            builtinTrigger = CCBTriggers.addSimple(id + "_builtin");
            mcBuilder.addCriterion("0", builtinTrigger.createCriterion(builtinTrigger.instance()));
        }

        CCBAdvancements.ENTRIES.add(this);
    }

    @Contract(pure = true)
    private @NotNull String titleKey() {
        return LANG + id;
    }

    @Contract(pure = true)
    private @NotNull String descriptionKey() {
        return titleKey() + ".desc";
    }

    boolean isAlreadyAwardedTo(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}
        if (serverPlayer.getServer() == null) {
            return false;
        }
        AdvancementHolder advancement = serverPlayer.getServer().getAdvancements().get(CreateCraftedBeginning.asResource(id));
		if (advancement == null) {
			return true;
		}
        return serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    void awardTo(Player player) {
		if (!(player instanceof ServerPlayer sp)) {
			return;
		}
		if (builtinTrigger == null) {
			return;
        }
        builtinTrigger.trigger(sp);
    }

    void save(Consumer<AdvancementHolder> t, HolderLookup.Provider registries) {
		if (parent != null) {
			mcBuilder.parent(parent.dataGenResult);
		}

		if (ccbBuilder.func != null) {
			ccbBuilder.icon(ccbBuilder.func.apply(registries));
		}

        mcBuilder.display(ccbBuilder.icon, Component.translatable(titleKey()), Component.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)), id.equals("root") ? BACKGROUND : null, ccbBuilder.type.advancementType, ccbBuilder.type.toast, ccbBuilder.type.announce, ccbBuilder.type.hide);

        dataGenResult = mcBuilder.save(t, CreateCraftedBeginning.asResource(id).toString());
    }

    void provideLang(@NotNull BiConsumer<String, String> consumer) {
        consumer.accept(titleKey(), title);
        consumer.accept(descriptionKey(), description);
    }

    public enum TaskType {

        SILENT(AdvancementType.TASK, false, false, false),
        NORMAL(AdvancementType.TASK, true, false, false),
        NOISY(AdvancementType.TASK, true, true, false),
        EXPERT(AdvancementType.GOAL, true, true, false),
        SECRET(AdvancementType.GOAL, true, true, true),
        CHALLENGE(AdvancementType.CHALLENGE, true, true, false),
        SECRET_CHALLENGE(AdvancementType.CHALLENGE, true, true, true);

        private final AdvancementType advancementType;
        private final boolean toast;
        private final boolean announce;
        private final boolean hide;

        TaskType(AdvancementType advancementType, boolean toast, boolean announce, boolean hide) {
            this.advancementType = advancementType;
            this.toast = toast;
            this.announce = announce;
            this.hide = hide;
        }
    }

    public class Builder {
        private CCBAdvancement.TaskType type = CCBAdvancement.TaskType.NORMAL;
        private boolean externalTrigger;
        private int keyIndex;
        private ItemStack icon;
        private Function<HolderLookup.Provider, ItemStack> func;

        CCBAdvancement.Builder special(CCBAdvancement.TaskType type) {
            this.type = type;
            return this;
        }

        CCBAdvancement.Builder after(CCBAdvancement other) {
            CCBAdvancement.this.parent = other;
            return this;
        }

        CCBAdvancement.Builder icon(@NotNull ItemProviderEntry<?, ?> item) {
            return icon(item.asStack());
        }

        CCBAdvancement.Builder icon(ItemLike item) {
            return icon(new ItemStack(item));
        }

        CCBAdvancement.Builder icon(ItemStack stack) {
            icon = stack;
            return this;
        }

        CCBAdvancement.Builder icon(Function<HolderLookup.Provider, ItemStack> func) {
            this.func = func;
            return this;
        }

        CCBAdvancement.Builder title(String title) {
            CCBAdvancement.this.title = title;
            return this;
        }

        CCBAdvancement.Builder description(String description) {
            CCBAdvancement.this.description = description;
            return this;
        }

        CCBAdvancement.Builder whenBlockPlaced(Block block) {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        CCBAdvancement.Builder whenIconCollected() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(icon.getItem()));
        }

        CCBAdvancement.Builder whenItemCollected(@NotNull ItemProviderEntry<?, ?> item) {
            return whenItemCollected(item.asStack().getItem());
        }

        CCBAdvancement.Builder whenItemCollected(ItemLike itemProvider) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(itemProvider));
        }

        CCBAdvancement.Builder whenItemCollected(TagKey<Item> tag) {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag).build()));
        }

        CCBAdvancement.Builder awardedForFree() {
            return externalTrigger(InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[]{}));
        }

        CCBAdvancement.Builder externalTrigger(Criterion<?> trigger) {
            mcBuilder.addCriterion(String.valueOf(keyIndex), trigger);
            externalTrigger = true;
            keyIndex++;
            return this;
        }
    }
}
