package net.ty.createcraftedbeginning.content.compressedair;

import com.simibubi.create.AllEnchantments;
import com.simibubi.create.content.fluids.tank.CreativeFluidTankBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CompressedAirCanisterItem extends Item {
    public static final int BAR_COLOR = 0xEFEFEF;
    public TagKey<Fluid> tagKey;
    public CompressedAirFakeFluid fluid;

    public CompressedAirCanisterItem(CompressedAirFakeFluid fluid, TagKey<Fluid> tagKey, Properties properties) {
        super(properties);
        this.fluid = fluid;
        this.tagKey = tagKey;
    }

    public static boolean isEmpty(ItemStack stack) {
        return get(stack) == 0;
    }

    public static boolean isFull(ItemStack stack) {
        return get(stack) == getCapacity(stack);
    }

    public static int get(ItemStack stack) {
        return stack.getOrDefault(CCBDataComponents.CANISTER_AIR, 0);
    }

    public static void set(ItemStack stack, int airAmount) {
        int clampedAir = Mth.clamp(airAmount, 0, getCapacity(stack));
        stack.set(CCBDataComponents.CANISTER_AIR, clampedAir);
    }

    public static void change(ItemStack stack, int delta) {
        int current = get(stack);
        set(stack, current + delta);
    }

    public static void fillAll(ItemStack stack) {
        change(stack, getCapacity(stack));
    }

    public static void drainAll(ItemStack stack) {
        set(stack, 0);
    }

    public static int getCapacity(ItemStack stack) {
        int enchantLevel = 0;
        ItemEnchantments enchants = stack.getTagEnchantments();
        for (Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            if (entry.getKey().is(AllEnchantments.CAPACITY)) {
                enchantLevel = entry.getIntValue();
                break;
            }
        }

        return CCBConfig.server().canisterCapacity.get() * 1000 * (1 + enchantLevel);
    }

    @SuppressWarnings("deprecation")
    public boolean isFluidValid(FluidStack stack) {
        return stack.getFluid().is(tagKey);
    }

    @Override
    public boolean supportsEnchantment(@NotNull ItemStack stack, Holder<Enchantment> enchantment) {
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
            return InteractionResult.CONSUME;
        }
        boolean isSneaking = player.isShiftKeyDown();

        if (!(stack.getItem() instanceof CompressedAirCanisterItem)) {
            return InteractionResult.CONSUME;
        }

        if (!(be instanceof CreativeFluidTankBlockEntity tankBlockEntity)) {
            return InteractionResult.CONSUME;
        }

        FluidTankBlockEntity controller = tankBlockEntity.getControllerBE();
        FluidTank tank = controller.getTankInventory();

        if (isSneaking) {
            if (CompressedAirCanisterItem.isEmpty(stack)) {
                return InteractionResult.FAIL;
            }

            CompressedAirCanisterItem.drainAll(stack);
        } else {
            if (tank.isEmpty() || CompressedAirCanisterItem.isFull(stack)) {
                return InteractionResult.FAIL;
            }

            if (!isFluidValid(tank.getFluid())) {
                return InteractionResult.FAIL;
            }
            CompressedAirCanisterItem.fillAll(stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(get(stack) / (float) getCapacity(stack), 0, 1));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return BAR_COLOR;
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

        MutableComponent fluidName = isEmpty(stack) ? CCBLang.translateDirect("tooltips.compressed_air_canister.content.empty") : CCBLang.fluidName(new FluidStack(fluid, 1)).component();

        tooltip.add(CCBLang.translateDirect("tooltips.compressed_air_canister.content").withStyle(ChatFormatting.GRAY).append(fluidName.withStyle(ChatFormatting.GOLD)));

        int airAmount = get(stack);
        tooltip.add(CCBLang.translateDirect("tooltips.compressed_air_canister.capacity").withStyle(ChatFormatting.GRAY).append(CCBLang.number(airAmount).add(CCBLang.text(" / ")).add(CCBLang.number(getCapacity(stack))).component().withStyle(ChatFormatting.AQUA)));
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return true;
    }
}
