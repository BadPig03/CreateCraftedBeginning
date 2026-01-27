package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.armhandlers.AirtightExtendArmHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AirtightExtendArmUtils {
    private static final Map<UUID, Boolean> CACHES_MAP = new HashMap<>();
    private static final Map<UUID, Boolean> OPERABILITIES_MAP = new HashMap<>();
    private static final Map<Gas, List<AttributeModifier>> MODIFIERS_MAP = new HashMap<>();

    static {
        initializeModifiersMap();
    }

    public static boolean getCached(@NotNull Player player) {
        return CACHES_MAP.getOrDefault(player.getUUID(), false);
    }

    public static void setCached(@NotNull Player player, boolean cached) {
        CACHES_MAP.put(player.getUUID(), cached);
    }

    public static boolean getOperability(@NotNull Player player) {
        return OPERABILITIES_MAP.getOrDefault(player.getUUID(), true);
    }

    public static void setOperability(@NotNull Player player, boolean operable) {
        OPERABILITIES_MAP.put(player.getUUID(), operable);
    }

    public static void refreshArmModifiers(Player player) {
        Gas gasType = CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType();
        if (gasType.isEmpty()) {
            if (!getCached(player)) {
                setCached(player, true);
                removeAllModifiers(player);
                return;
            }
            return;
        }

        if (!canIncreaseRange(player)) {
            removeModifiers(player, gasType);
            return;
        }

        addModifiers(player, gasType);
    }

    public static void addModifiers(@NotNull Player player, Gas gasType) {
        AttributeInstance blockInstance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entityInstance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance knockbackInstance = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        if (blockInstance == null || entityInstance == null || knockbackInstance == null) {
            return;
        }

        List<AttributeModifier> modifiers = MODIFIERS_MAP.get(gasType);
        if (modifiers == null || modifiers.size() != 3) {
            return;
        }

        AttributeModifier blockModifier = modifiers.getFirst();
        if (!blockInstance.hasModifier(blockModifier.id())) {
            blockInstance.addTransientModifier(blockModifier);
        }

        AttributeModifier entityModifier = modifiers.get(1);
        if (!entityInstance.hasModifier(entityModifier.id())) {
            entityInstance.addTransientModifier(entityModifier);
        }

        AttributeModifier knockbackModifier = modifiers.getLast();
        if (!knockbackInstance.hasModifier(knockbackModifier.id())) {
            knockbackInstance.addTransientModifier(knockbackModifier);
        }
    }

    public static void removeModifiers(@NotNull Player player, Gas gasType) {
        AttributeInstance blockInstance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entityInstance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance knockbackInstance = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        if (blockInstance == null || entityInstance == null || knockbackInstance == null) {
            return;
        }

        List<AttributeModifier> modifiers = MODIFIERS_MAP.get(gasType);
        if (modifiers == null || modifiers.size() != 3) {
            return;
        }

        AttributeModifier blockModifier = modifiers.getFirst();
        if (blockInstance.hasModifier(blockModifier.id())) {
            blockInstance.removeModifier(blockModifier);
        }

        AttributeModifier entityModifier = modifiers.get(1);
        if (entityInstance.hasModifier(entityModifier.id())) {
            entityInstance.removeModifier(entityModifier);
        }

        AttributeModifier knockbackModifier = modifiers.getLast();
        if (knockbackInstance.hasModifier(knockbackModifier.id())) {
            knockbackInstance.removeModifier(knockbackModifier);
        }
    }

    public static void removeAllModifiers(@NotNull Player player) {
        AttributeInstance blockInstance = player.getAttributes().getInstance(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entityInstance = player.getAttributes().getInstance(Attributes.ENTITY_INTERACTION_RANGE);
        AttributeInstance knockbackInstance = player.getAttributes().getInstance(Attributes.ATTACK_KNOCKBACK);
        if (blockInstance == null || entityInstance == null || knockbackInstance == null) {
            return;
        }

        MODIFIERS_MAP.values().stream().filter(modifiers -> modifiers != null && modifiers.size() == 3).forEach(modifiers -> {
            AttributeModifier blockModifier = modifiers.getFirst();
            if (blockInstance.hasModifier(blockModifier.id())) {
                blockInstance.removeModifier(blockModifier);
            }

            AttributeModifier entityModifier = modifiers.get(1);
            if (entityInstance.hasModifier(entityModifier.id())) {
                entityInstance.removeModifier(entityModifier);
            }

            AttributeModifier knockbackModifier = modifiers.getLast();
            if (knockbackInstance.hasModifier(knockbackModifier.id())) {
                knockbackInstance.removeModifier(knockbackModifier);
            }
        });
    }

    public static boolean isHoldingArms(@NotNull Player player) {
        return player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM);
    }

    public static boolean canIncreaseRange(@NotNull Player player) {
        if (!isHoldingArms(player) || !getOperability(player) || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        Gas gasType = gasContent.getGasType();
        if (gasContent.isEmpty()) {
            return false;
        }

        AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
        return armHandler != null;
    }

    public static boolean updateOperationAbility(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        Gas gasType = gasContent.getGasType();
        if (gasContent.isEmpty()) {
            return false;
        }

        AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            return false;
        }

        int gasConsumption = Mth.ceil(CCBConfig.server().equipments.armCostPerUse.get() * armHandler.getGasConsumptionMultiplier());
        Level level = player.level();
        return CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, () -> !level.isClientSide);
    }

    @OnlyIn(Dist.CLIENT)
    public static void appendHoverText(ItemStack arm, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        Gas gasType = gasContent.getGasType();
        if (gasContent.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());

        AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
        if (armHandler == null) {
            return;
        }

        armHandler.appendHoverText(arm, context, tooltip, tooltipFlag);
    }

    private static void initializeModifiersMap() {
        Registry<Gas> gasRegistry = CCBGasRegistries.GAS_REGISTRY;
        for (Gas gasType : gasRegistry) {
            if (gasType.isEmpty()) {
                continue;
            }

            AirtightExtendArmHandler armHandler = AirtightExtendArmHandler.REGISTRY.get(gasType);
            if (armHandler == null) {
                continue;
            }

            ResourceLocation modifierID = CreateCraftedBeginning.asResource("extend_arm_" + gasRegistry.getKey(gasType).getPath().replace('/', '_'));
            AttributeModifier blockModifier = new AttributeModifier(modifierID, armHandler.getIncreasedBlockInteractionRange(), Operation.ADD_VALUE);
            AttributeModifier entityModifier = new AttributeModifier(modifierID, armHandler.getIncreasedEntityInteractionRange(), Operation.ADD_VALUE);
            AttributeModifier knockbackModifier = new AttributeModifier(modifierID, armHandler.getIncreasedKnockback(), Operation.ADD_VALUE);
            MODIFIERS_MAP.put(gasType, List.of(blockModifier, entityModifier, knockbackModifier));
        }
    }
}
