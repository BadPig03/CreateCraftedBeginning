package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.weatherflares.WeatherFlareSupplierUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonItem extends ProjectileWeaponItem implements CustomArmPoseItem {
    private static final int MAX_CHARGE_TIME = 72000;

    public AirtightCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level world, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        return CanisterContainerSuppliers.isAnyContainerAvailable(player) ? use(context.getLevel(), player, context.getHand()).getResult() : InteractionResult.FAIL;
    }

    @Override
    public float getDestroySpeed(ItemStack cannon, BlockState state) {
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cannon = player.getItemInHand(hand);
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player) || CanisterContainerSuppliers.getFirstAvailableGasContent(player) == GasStack.EMPTY) {
            return InteractionResultHolder.fail(cannon);
        }
        if (ShootableGadgetItemMethods.shouldSwap(player, cannon, hand, stack -> stack.is(CCBItems.AIRTIGHT_CANNON))) {
            return InteractionResultHolder.fail(cannon);
        }

        player.startUsingItem(hand);
        if (level.isClientSide) {
            CreateCraftedBeginningClient.AIRTIGHT_CANNON_RENDER_HANDLER.dontAnimateItem(hand);
        }

        return InteractionResultHolder.success(cannon);
    }

    @Override
    public boolean isBarVisible(ItemStack cannon) {
        return CanisterContainerClients.isBarVisible();
    }

    @Override
    public int getBarWidth(ItemStack cannon) {
        return CanisterContainerClients.getBarWidth();
    }

    @Override
    public int getBarColor(ItemStack cannon) {
        return CanisterContainerClients.getBarColor();
    }

    @Override
    public boolean mineBlock(ItemStack cannon, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return false;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack cannon) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack cannon, LivingEntity entity) {
        return MAX_CHARGE_TIME;
    }

    @Override
    public void releaseUsing(ItemStack cannon, Level level, LivingEntity entity, int timeCharged) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }

        float chargedRatio = AirtightCannonUtils.getChargedRatio(cannon, player, MAX_CHARGE_TIME - timeCharged);
        if (chargedRatio == -1) {
            return;
        }

        ItemStack flareItem = WeatherFlareSupplierUtils.getFirstFlare(player);
        if (!flareItem.isEmpty()) {
            AirtightCannonUtils.fireFlares(level, player, flareItem, chargedRatio);
            return;
        }

        AirtightCannonUtils.spawnWindCharges(level, player, chargedRatio);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(CCBLang.gasName(gasContent).add(CCBLang.translate("gui.tooltips.gas_tools.content")).style(ChatFormatting.GRAY).component());
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasContent.getGasType());
        if (cannonHandler == null) {
            return;
        }

        cannonHandler.appendHoverText(cannon, context, tooltip, tooltipFlag);
        float consumptionMultiplier = cannonHandler.getGasConsumptionMultiplier();
        MutableComponent advancedConsumptionMultiplier = tooltipFlag.isAdvanced() ? CCBLang.text(" [x" + cannonHandler.getRenderStr(consumptionMultiplier) + ']').component() : Component.empty();
        tooltip.add(CCBLang.translate("gui.tooltips.gas_tools.gas_consumption", cannonHandler.getRenderStr(consumptionMultiplier * 100)).add(advancedConsumptionMultiplier.withStyle(ChatFormatting.GRAY)).style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public boolean isEnchantable(ItemStack cannon) {
        return true;
    }

    @Override
    public boolean isValidRepairItem(ItemStack cannon, ItemStack repair) {
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack cannon, LivingEntity entity, InteractionHand hand) {
        return true;
    }

    @Override
    public boolean supportsEnchantment(ItemStack cannon, Holder<Enchantment> enchantment) {
        return !enchantment.is(Enchantments.PIERCING) && super.supportsEnchantment(cannon, enchantment);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || newStack.getItem() != oldStack.getItem();
    }

    @Override
    public boolean isDamageable(ItemStack cannon) {
        return false;
    }

    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ItemStack::isEmpty;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 15;
    }

    @Override
    protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectileItems, float velocity, float inaccuracy, boolean isCrit, @Nullable LivingEntity target) {
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target) {
    }

    @Override
    @Nullable
    public ArmPose getArmPose(ItemStack cannon, AbstractClientPlayer player, InteractionHand hand) {
        return player.swinging ? null : ArmPose.CROSSBOW_HOLD;
    }
}
