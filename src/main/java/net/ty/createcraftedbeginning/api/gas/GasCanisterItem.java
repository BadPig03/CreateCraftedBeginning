package net.ty.createcraftedbeginning.api.gas;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBGases;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class GasCanisterItem extends Item {
    public GasCanisterItem(Properties properties) {
        super(properties);
    }

    public static @NotNull GasStack getContent(@NotNull ItemStack stack) {
        return stack.getOrDefault(CCBDataComponents.CANISTER_CONTENT, GasStack.EMPTY);
    }

    public static long getGasAmount(ItemStack stack) {
        GasStack content = getContent(stack);
        return content == GasStack.EMPTY ? 0 : content.getAmount();
    }

    public static void setGasContent(@NotNull ItemStack stack, GasStack gasStack) {
        stack.set(CCBDataComponents.CANISTER_CONTENT, gasStack);
    }

    public static void clearGasContent(@NotNull ItemStack stack) {
        stack.set(CCBDataComponents.CANISTER_CONTENT, GasStack.EMPTY);
    }

    public static void setGasAmount(ItemStack stack, Gas gas, long amount) {
        if (isEmpty(stack)) {
            setGasContent(stack, new GasStack(gas.getHolder(), amount));
            return;
        }
        GasStack content = getContent(stack);
        if (amount <= 0) {
            clearGasContent(stack);
        }
        long capacity = getCapacity(stack);
        content.setAmount(Math.min(capacity, amount));
    }

    public static long addGasAmount(ItemStack stack, Gas gas, long amount) {
        long capacity = getCapacity(stack);

        if (isEmpty(stack)) {
            long actualAmount = Mth.clamp(amount, 0, capacity);
            setGasContent(stack, new GasStack(gas.getHolder(), actualAmount));
            return actualAmount;
        }

        GasStack content = getContent(stack);
        if (!GasStack.isSameGas(content, gas)) {
            return 0;
        }

        long current = content.getAmount();
        long newAmount = Mth.clamp(current + amount, 0, capacity);
        setGasContent(stack, content.copyWithAmount(newAmount));

        return newAmount - current;
    }

    public static boolean isEmpty(ItemStack stack) {
        return getContent(stack).isEmpty();
    }

    public static boolean isFull(ItemStack stack) {
        return getContent(stack).getAmount() == getCapacity(stack);
    }

    public static long getCapacity(@NotNull ItemStack stack) {
        int enchantLevel = 0;
        ItemEnchantments enchants = stack.getTagEnchantments();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            if (entry.getKey().is(AllEnchantments.CAPACITY)) {
                enchantLevel = entry.getIntValue();
                break;
            }
        }

        return CCBConfig.server().compressedAir.canisterCapacity.get() * 1000L * (1 + enchantLevel);
    }

    public static boolean isGasValid(ItemStack itemStack, GasStack gasStack) {
        GasStack content = getContent(itemStack);
        return content.isEmpty() || GasStack.isSameGas(content, gasStack);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, @NotNull Holder<Enchantment> enchantment) {
        if (enchantment.is(AllEnchantments.CAPACITY)) {
            return true;
        }
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext ctx) {
        BlockPos clickPos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        Player player = ctx.getPlayer();
        BlockEntity be = level.getBlockEntity(clickPos);

        if (player == null || player instanceof FakePlayer) {
            return InteractionResult.FAIL;
        }

        if (!(stack.getItem() instanceof GasCanisterItem)) {
            return InteractionResult.FAIL;
        }

        if (be instanceof AirtightTankBlockEntity tankBlockEntity) {
            return getAirtightTankInteractionResult(tankBlockEntity, player, stack);
        } else if (be instanceof CreativeAirtightTankBlockEntity tankBlockEntity) {
            return getCreativeAirtightTankInteractionResult(tankBlockEntity, player, stack, level);
        }

        return InteractionResult.FAIL;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13f * Mth.clamp((float) getContent(stack).getAmount() / getCapacity(stack), 0, 1));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        float airRatio = (float) getContent(stack).getAmount() / getCapacity(stack);
        if (airRatio > 0.1f) {
            return 0xFF71C7D5;
        } else {
            return 0xFFEFEFEF;
        }
    }

    @Override
    public @NotNull String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            super.appendHoverText(stack, context, tooltip, flag);
            return;
        }

        super.appendHoverText(stack, context, tooltip, flag);
        GasStack content = getContent(stack);
        long airAmount = content.getAmount();
        MutableComponent mb = CCBLang.translateDirect("gui.goggles.unit.milli_buckets");
        MutableComponent gasName = isEmpty(stack) ? CCBLang.translateDirect("tooltips.gas_canister.content.empty") : CCBLang.gasName(getContent(stack)).component();
        tooltip.add(CCBLang.translateDirect("tooltips.gas_canister.content").withStyle(ChatFormatting.GRAY).append(gasName.withStyle(ChatFormatting.GOLD)));
        tooltip.add(CCBLang.builder().add(CCBLang.translateDirect("tooltips.gas_canister.capacity").withStyle(ChatFormatting.GRAY)).add(CCBLang.number(airAmount).add(mb).component().withStyle(ChatFormatting.AQUA)).add(CCBLang.text(" / ").component().withStyle(ChatFormatting.GRAY)).add(CCBLang.number(getCapacity(stack)).add(mb).component().withStyle(ChatFormatting.DARK_AQUA)).component());

        Gas pressurizedGas = content.getGas().getPressurizedGas();
        if (pressurizedGas != null) {
            tooltip.add(CCBLang.translateDirect("tooltips.gas_canister.pressurized_gas").withStyle(ChatFormatting.GRAY).append(Component.translatable(pressurizedGas.getTranslationKey()).withStyle(ChatFormatting.GOLD)));
        }

        Gas depressurizedGas = content.getGas().getDepressurizedGas();
        if (depressurizedGas != null) {
            tooltip.add(CCBLang.translateDirect("tooltips.gas_canister.depressurized_gas").withStyle(ChatFormatting.GRAY).append(Component.translatable(depressurizedGas.getTranslationKey()).withStyle(ChatFormatting.GOLD)));
        }

        Gas vortexedGas = content.getGas().getVortexedGasName();
        if (vortexedGas != null) {
            tooltip.add(CCBLang.translateDirect("tooltips.gas_canister.vortexed_gas").withStyle(ChatFormatting.GRAY).append(Component.translatable(vortexedGas.getTranslationKey()).withStyle(ChatFormatting.GOLD)));
        }
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        if (!isEmpty(stack)) {
            GasStack gasStack = getContent(stack);
            if (gasStack.is(CCBGases.NATURAL_AIR)) {
                return true;
            }
        }
        return super.isFoil(stack);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }

    private InteractionResult getCreativeAirtightTankInteractionResult(@NotNull CreativeAirtightTankBlockEntity tankBlockEntity, Player player, ItemStack stack, Level level) {
        CreativeAirtightTankBlockEntity controller = tankBlockEntity.getControllerBE();
        if (controller == null) {
            return InteractionResult.FAIL;
        }

        IGasHandler gasHandler = level.getCapability(GasCapabilities.GasHandler.BLOCK, controller.getBlockPos(), null);
        if (gasHandler == null) {
            return InteractionResult.FAIL;
        }
        GasStack previousGasInTank = gasHandler.getGasInTank(0).copy();

        if (!(gasHandler instanceof CreativeSmartGasTank creativeTankHandler)) {
            return InteractionResult.FAIL;
        }

        GasStack gasStack = getContent(stack);
        if (gasStack.isEmpty()) {
            if (creativeTankHandler.isEmpty()) {
                return InteractionResult.FAIL;
            }
            creativeTankHandler.setContainedGas(GasStack.EMPTY);
            return InteractionResult.SUCCESS;
        }

        creativeTankHandler.setContainedGas(gasStack);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult getAirtightTankInteractionResult(@NotNull AirtightTankBlockEntity tankBlockEntity, Player player, ItemStack stack) {
        AirtightTankBlockEntity controller = tankBlockEntity.getControllerBE();
        if (controller == null) {
            return InteractionResult.FAIL;
        }

        GasTank tank = controller.getTankInventory();
        if (player.isShiftKeyDown()) {
            if (isEmpty(stack)) {
                return InteractionResult.FAIL;
            }

            GasStack gasStack = getContent(stack);
            if (!tank.isGasValid(0, gasStack)) {
                return InteractionResult.FAIL;
            }

            long maxTransfer = Math.min(gasStack.getAmount(), tank.getSpace());
            long actuallyAdded = tank.fill(gasStack.copyWithAmount(maxTransfer), GasAction.SIMULATE);

            if (actuallyAdded > 0) {
                tank.fill(new GasStack(gasStack.getGasHolder(), actuallyAdded), GasAction.EXECUTE);
                addGasAmount(stack, gasStack.getGas(), -actuallyAdded);
                return InteractionResult.SUCCESS;
            }
        } else {
            if (tank.isEmpty() || isFull(stack)) {
                return InteractionResult.FAIL;
            }

            GasStack tankGas = tank.getGas();
            if (!isGasValid(stack, tankGas)) {
                return InteractionResult.FAIL;
            }

            long spaceLeft = getCapacity(stack) - getGasAmount(stack);
            if (spaceLeft <= 0) {
                return InteractionResult.FAIL;
            }

            GasStack drained = tank.drain(spaceLeft, GasAction.SIMULATE);
            if (drained.isEmpty() || drained.getAmount() <= 0) {
                return InteractionResult.FAIL;
            }

            drained = tank.drain(drained.getAmount(), GasAction.EXECUTE);
            long added = addGasAmount(stack, drained.getGas(), drained.getAmount());
            if (added == drained.getAmount()) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }
}
