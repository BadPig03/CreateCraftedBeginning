package net.ty.createcraftedbeginning.content.airtights.gascanister;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTank;
import net.ty.createcraftedbeginning.api.gas.gases.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBEnchantments;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GasCanisterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Nameable {
    private static final Component DEFAULT_NAME = CCBItems.GAS_CANISTER.get().getDescription();
    private static final String COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL = "CapacityEnchantLevel";
    private static final String COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL = "EconomizeEnchantLevel";
    private static final String COMPOUND_KEY_CUSTOM_NAME = "CustomName";
    private static final String COMPOUND_KEY_COMPONENTS_PATCH = "ComponentsPatch";

    private DataComponentPatch componentPatch;
    private SmartGasTankBehaviour tankBehaviour;
    private Component customName;
    private int capacityEnchantLevel;
    private int economizeEnchantLevel;

    public GasCanisterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        componentPatch = DataComponentPatch.EMPTY;
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.GAS_CANISTER.get(), (be, context) -> be.tankBehaviour.getCapability());
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        tankBehaviour = SmartGasTankBehaviour.single(this, getMaxCapacity()).forbidInsertion().forbidExtraction();
        behaviours.add(tankBehaviour);
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putInt(COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL, capacityEnchantLevel);
        compoundTag.putInt(COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL, economizeEnchantLevel);
        compoundTag.put(COMPOUND_KEY_COMPONENTS_PATCH, CatnipCodecUtils.encode(DataComponentPatch.CODEC, provider, componentPatch).orElse(new CompoundTag()));
        if (customName == null) {
            return;
        }

        compoundTag.putString(COMPOUND_KEY_CUSTOM_NAME, Serializer.toJson(customName, provider));
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL)) {
            capacityEnchantLevel = compoundTag.getInt(COMPOUND_KEY_CAPACITY_ENCHANT_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL)) {
            economizeEnchantLevel = compoundTag.getInt(COMPOUND_KEY_ECONOMIZE_ENCHANT_LEVEL);
        }
        if (compoundTag.contains(COMPOUND_KEY_CUSTOM_NAME)) {
            customName = Serializer.fromJson(compoundTag.getString(COMPOUND_KEY_CUSTOM_NAME), provider);
        }
        componentPatch = CatnipCodecUtils.decode(DataComponentPatch.CODEC, provider, compoundTag.getCompound(COMPOUND_KEY_COMPONENTS_PATCH)).orElse(DataComponentPatch.EMPTY);
        tankBehaviour.getPrimaryHandler().setCapacity(getMaxCapacity());
        notifyUpdate();
    }

    public DataComponentPatch getComponentPatch() {
        return componentPatch;
    }

    public long getMaxCapacity() {
        return CCBConfig.server().gas.canisterCapacity.get() * 1000L * (1 + capacityEnchantLevel);
    }

    public void setContent(@NotNull ItemStack canister, @NotNull Level level) {
        capacityEnchantLevel = canister.getEnchantmentLevel(level.holderOrThrow(AllEnchantments.CAPACITY));
        economizeEnchantLevel = canister.getEnchantmentLevel(level.holderOrThrow(CCBEnchantments.ECONOMIZE));
        componentPatch = canister.getComponentsPatch();
        if (canister.has(DataComponents.CUSTOM_NAME)) {
            customName = canister.getHoverName();
        }
        tankBehaviour.getPrimaryHandler().setCapacity(getMaxCapacity());
        GasStack gasStack = canister.getOrDefault(CCBDataComponents.CANISTER_CONTENT, GasStack.EMPTY);
        tankBehaviour.getInternalGasHandler().forceFill(gasStack, GasAction.EXECUTE);
        notifyUpdate();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (level == null) {
            return false;
        }

        SmartGasTank gasTank = tankBehaviour.getPrimaryHandler();
        if (gasTank == null) {
            return false;
        }

        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        CCBLang.translate("gui.goggles.gas_container").forGoggles(tooltip);
        GasStack stack = gasTank.getGasStack();
        if (stack.isEmpty()) {
            CCBLang.translate("gui.goggles.gas_container.capacity").add(CCBLang.number(gasTank.getCapacity()).add(mb).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.gasName(stack).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(stack.getAmount()).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(gasTank.getCapacity()).add(mb).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    @Override
    public @NotNull Component getName() {
        return customName == null ? DEFAULT_NAME : customName;
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput componentInput) {
    }

    @Override
    protected void collectImplicitComponents(@NotNull Builder components) {
        components.set(CCBDataComponents.CANISTER_CONTENT, getContent());
    }

    public GasStack getContent() {
        return tankBehaviour.getPrimaryHandler().getGasStack();
    }
}
