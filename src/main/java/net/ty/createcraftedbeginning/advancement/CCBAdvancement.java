package net.ty.createcraftedbeginning.advancement;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.core.HolderLookup.Provider;
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
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public class CCBAdvancement {
    private static final ResourceLocation BACKGROUND = CreateCraftedBeginning.asResource("textures/gui/advancements.png");
    private final Advancement.Builder advancementBuilder = Advancement.Builder.advancement();
    private final Builder builder = new Builder();
    private final String id;

    private AdvancementHolder dataGenResult;
    private SimpleCCBTrigger builtinTrigger;
    private CCBAdvancement parent;
    private String title;
    private String description;

    public CCBAdvancement(String id, @NotNull UnaryOperator<Builder> operator) {
        this.id = id;
        operator.apply(builder);
        if (!builder.externalTrigger) {
            builtinTrigger = CCBTriggers.addSimple(id + "_builtin");
            advancementBuilder.addCriterion("0", builtinTrigger.createCriterion(SimpleCCBTrigger.instance()));
        }
        CCBAdvancements.ENTRIES.add(this);
    }

    public boolean isAlreadyAwardedTo(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        if (serverPlayer.getServer() == null) {
            return false;
        }

        AdvancementHolder advancement = serverPlayer.getServer().getAdvancements().get(CreateCraftedBeginning.asResource(id));
        return advancement == null || serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone();

    }

    public void awardTo(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || builtinTrigger == null) {
            return;
        }

        builtinTrigger.trigger(serverPlayer);
    }

    public void save(Consumer<AdvancementHolder> consumer, Provider provider) {
        if (parent != null) {
            advancementBuilder.parent(parent.dataGenResult);
        }
        if (builder.func != null) {
            builder.icon(builder.func.apply(provider));
        }

        advancementBuilder.display(builder.icon, Component.translatable(titleKey()), Component.translatable(descriptionKey()).withStyle(s -> s.withColor(0xDBA213)), "root".equals(id) ? BACKGROUND : null, builder.type.advancementType, builder.type.toast, builder.type.announce, builder.type.hide);
        dataGenResult = advancementBuilder.save(consumer, CreateCraftedBeginning.asResource(id).toString());
    }

    @Contract(pure = true)
    private @NotNull String titleKey() {
        return "advancement." + CreateCraftedBeginning.MOD_ID + '.' + id;
    }

    @Contract(pure = true)
    private @NotNull String descriptionKey() {
        return titleKey() + ".desc";
    }

    public void provideLang(@NotNull BiConsumer<String, String> consumer) {
        consumer.accept(titleKey(), title);
        consumer.accept(descriptionKey(), description);
    }

    public enum TaskType {
        HIDDEN_TASK(AdvancementType.TASK, false, false, false),
        NORMAL_TASK(AdvancementType.TASK, true, false, false),
        ANNOUNCED_TASK(AdvancementType.TASK, true, true, false),
        GOAL(AdvancementType.GOAL, true, true, false),
        HIDDEN_GOAL(AdvancementType.GOAL, true, true, true),
        CHALLENGE(AdvancementType.CHALLENGE, true, true, false),
        HIDDEN_CHALLENGE(AdvancementType.CHALLENGE, true, true, true);

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
        private TaskType type = TaskType.NORMAL_TASK;
        private boolean externalTrigger;
        private int keyIndex;
        private ItemStack icon;
        private Function<Provider, ItemStack> func;

        public Builder special(TaskType type) {
            this.type = type;
            return this;
        }

        public Builder after(CCBAdvancement other) {
            parent = other;
            return this;
        }

        public Builder icon(@NotNull ItemProviderEntry<?, ?> item) {
            return icon(item.asStack());
        }

        public Builder icon(ItemStack item) {
            icon = item;
            return this;
        }

        public Builder icon(ItemLike item) {
            return icon(new ItemStack(item));
        }

        public Builder icon(Function<Provider, ItemStack> func) {
            this.func = func;
            return this;
        }

        public Builder title(String title) {
            CCBAdvancement.this.title = title;
            return this;
        }

        public Builder description(String description) {
            CCBAdvancement.this.description = description;
            return this;
        }

        public Builder whenBlockPlaced(Block block) {
            return externalTrigger(ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(block));
        }

        public Builder externalTrigger(Criterion<?> trigger) {
            advancementBuilder.addCriterion(String.valueOf(keyIndex), trigger);
            externalTrigger = true;
            keyIndex++;
            return this;
        }

        public Builder whenIconCollected() {
            return externalTrigger(TriggerInstance.hasItems(icon.getItem()));
        }

        public Builder whenItemCollected(@NotNull ItemProviderEntry<?, ?> item) {
            return whenItemCollected(item.asStack().getItem());
        }

        public Builder whenItemCollected(ItemLike itemProvider) {
            return externalTrigger(TriggerInstance.hasItems(itemProvider));
        }

        public Builder whenItemCollected(TagKey<Item> tag) {
            return externalTrigger(TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag).build()));
        }

        public Builder awardedForFree() {
            return externalTrigger(TriggerInstance.hasItems(new ItemLike[]{}));
        }
    }
}
