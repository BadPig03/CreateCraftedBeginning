package net.ty.createcraftedbeginning.content.compressedair;

import com.simibubi.create.AllEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBDistExecutor;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class CanisterUtil {
    private static final List<Function<LivingEntity, List<ItemStack>>> MEDIUM_CANISTER_SUPPLIERS = new ArrayList<>();
    private static final List<Function<LivingEntity, List<ItemStack>>> HIGH_CANISTER_SUPPLIERS = new ArrayList<>();

    static {
        addMediumCanisterSupplier(entity -> {
            List<ItemStack> stacks = new ArrayList<>();
            if (!(entity instanceof Player player)) {
                return stacks;
            }
            stacks.addAll(getValidStacks(player, false));
            return stacks;
        });
    }

    static {
        addHighCanisterSupplier(entity -> {
            List<ItemStack> stacks = new ArrayList<>();
            if (!(entity instanceof Player player)) {
                return stacks;
            }
            stacks.addAll(getValidStacks(player, true));
            return stacks;
        });
    }

    public static boolean isInjectableCanister(ItemStack itemStack) {
        return (isValidCanister(itemStack, null)) && !isFullAir(itemStack);
    }

    public static boolean isValidCanister(ItemStack itemStack, Boolean isHigh) {
        if (itemStack.isEmpty()) {
            return false;
        }
        if (isHigh == null) {
            return CCBTags.CCBItemTags.COMPRESSED_AIR_CANISTER.matches(itemStack);
        }
        return isHigh ? CCBTags.CCBItemTags.HIGH_PRESSURE_COMPRESSED_AIR_CANISTER.matches(itemStack) : CCBTags.CCBItemTags.MEDIUM_PRESSURE_COMPRESSED_AIR_CANISTER.matches(itemStack);
    }

    public static List<ItemStack> getAllWithAir(LivingEntity entity) {
        List<ItemStack> highCanisters = new ArrayList<>();
        List<ItemStack> mediumCanisters = new ArrayList<>();

        for (Function<LivingEntity, List<ItemStack>> supplier : HIGH_CANISTER_SUPPLIERS) {
            for (ItemStack stack : supplier.apply(entity)) {
                if (hasAirRemaining(stack)) {
                    highCanisters.add(stack);
                }
            }
        }

        for (Function<LivingEntity, List<ItemStack>> supplier : MEDIUM_CANISTER_SUPPLIERS) {
            for (ItemStack stack : supplier.apply(entity)) {
                if (hasAirRemaining(stack)) {
                    mediumCanisters.add(stack);
                }
            }
        }

        highCanisters.sort(Comparator.comparingInt(CanisterUtil::getAir));
        mediumCanisters.sort(Comparator.comparingInt(CanisterUtil::getAir));

        List<ItemStack> all = new ArrayList<>(highCanisters);
        all.addAll(mediumCanisters);
        return all;
    }

    public static boolean hasAirRemaining(ItemStack canister) {
        return getAir(canister) > 0;
    }

    public static int getAirUsed(ItemStack canister) {
        return maxAir(canister) - getAir(canister);
    }

    public static boolean isFullAir(ItemStack canister) {
        return getAir(canister) == maxAir(canister);
    }

    public static int getAir(ItemStack canister) {
        int air = canister.getOrDefault(CCBDataComponents.CANISTER_AIR, 0);
        return Math.min(air, maxAir(canister));
    }

    public static void consumeAir(ItemStack canister, int i) {
        int maxAir = maxAir(canister);
        int air = getAir(canister);
        int newAir = Math.max(air - i, 0);
        canister.set(CCBDataComponents.CANISTER_AIR, Math.min(newAir, maxAir));
    }

    public static int maxAir(ItemStack canister) {
        int enchantLevel = 0;
        ItemEnchantments enchants = canister.getTagEnchantments();
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            if (entry.getKey().is(AllEnchantments.CAPACITY)) {
                enchantLevel = entry.getIntValue();
                break;
            }
        }
        return maxAir(enchantLevel);
    }

    public static int maxAir(int enchantLevel) {
        return CCBConfig.server().compressedAir.canisterCapacity.get() * 1000 * (1 + enchantLevel);
    }

    public static int maxAirWithoutEnchants() {
        return CCBConfig.server().compressedAir.canisterCapacity.get() * 1000;
    }

    public static boolean canAbsorbDamage(LivingEntity entity, int usesPerTank) {
        if (usesPerTank == 0) {
            return true;
        }

        if (entity instanceof Player && ((Player) entity).isCreative()) {
            return true;
        }

        List<ItemStack> canisters = getAllWithAir(entity);
        if (canisters.isEmpty()) {
            return false;
        }

        for (ItemStack canister : canisters) {
            if (!isValidCanister(canister, true)) {
                continue;
            }

            int cost = Math.max(maxAirWithoutEnchants() / usesPerTank, 1);
            consumeAir(canister, cost);
            return true;
        }

        for (ItemStack canister : canisters) {
            if (!isValidCanister(canister, false)) {
                continue;
            }

            int cost = Math.max(maxAirWithoutEnchants() / usesPerTank, 1);
            consumeAir(canister, cost);
            return true;
        }

        return false;
    }

    public static boolean isBarVisible(ItemStack stack, int usesPerTank) {
        if (usesPerTank == 0) {
            return false;
        }

        Player player = CCBDistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
        if (player == null) {
            return false;
        }

        List<ItemStack> canisters = getAllWithAir(player);
        if (canisters.isEmpty()) {
            return stack.isDamaged();
        }

        return true;
    }

    public static int getBarWidth(ItemStack stack, int usesPerTank) {
        if (usesPerTank == 0) {
            return 13;
        }

        Player player = CCBDistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
        if (player == null) {
            return 13;
        }

        List<ItemStack> canisters = getAllWithAir(player);
        if (canisters.isEmpty()) {
            return Math.round(13f - (float) stack.getDamageValue() / stack.getMaxDamage() * 13f);
        }

        if (canisters.size() == 1) {
            return canisters.getFirst().getItem().getBarWidth(canisters.getFirst());
        }

        int sumBarWidth = canisters.stream().map(backtank -> backtank.getItem().getBarWidth(backtank)).reduce(0, Integer::sum);
        return Math.round((float) sumBarWidth / canisters.size());
    }

    public static int getBarColor(ItemStack stack, int usesPerTank) {
        if (usesPerTank == 0) {
            return 0;
        }

        Player player = CCBDistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
        if (player == null) {
            return 0;
        }

        List<ItemStack> canisters = getAllWithAir(player);
        if (canisters.isEmpty()) {
            return Mth.hsvToRgb(Math.max(0, 1f - (float) stack.getDamageValue() / stack.getMaxDamage()) / 3f, 1f, 1f);
        }

        return canisters.getFirst().getItem().getBarColor(canisters.getFirst());
    }

    public static void addMediumCanisterSupplier(Function<LivingEntity, List<ItemStack>> supplier) {
        MEDIUM_CANISTER_SUPPLIERS.add(supplier);
    }

    public static void addHighCanisterSupplier(Function<LivingEntity, List<ItemStack>> supplier) {
        HIGH_CANISTER_SUPPLIERS.add(supplier);
    }

    private static List<ItemStack> getValidStacks(Player player, boolean isHigh) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if (isValidCanister(itemStack, isHigh)) {
                stacks.add(itemStack);
            }
        }
        ItemStack offhandItemStack = player.getOffhandItem();
        if (isValidCanister(offhandItemStack, isHigh)) {
            stacks.add(offhandItemStack);
        }
        return stacks;
    }
}
