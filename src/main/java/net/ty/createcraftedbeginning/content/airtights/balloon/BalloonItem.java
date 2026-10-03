package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BalloonItem extends PackageItem {
    private static final double MIN_RELEASE_VELOCITY = 0.1;
    private static final ResourceLocation END_VISIT_ADVANCEMENT = ResourceLocation.withDefaultNamespace("story/enter_the_end");

    private final boolean rare;

    public BalloonItem(Properties properties, PackageStyle style, boolean rare) {
        super(properties, style);
        this.rare = rare;
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.STANDARD_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
        if (rare) {
            BalloonStyles.RARE_BALLOONS.add(this);
            return;
        }

        BalloonStyles.REGULAR_BALLOONS.add(this);
    }

    public static boolean containsGas(ItemStack stack) {
        return !getGas(stack).isEmpty();
    }

    public static boolean isBalloon(ItemStack stack) {
        return stack.getItem() instanceof BalloonItem;
    }

    public static GasStack getGas(ItemStack stack) {
        if (!isBalloon(stack)) {
            return GasStack.EMPTY;
        }

        GasStack gas = stack.getOrDefault(CCBDataComponents.BALLOON_GAS, GasStack.EMPTY);
        if (gas.isEmpty()) {
            return GasStack.EMPTY;
        }

        return gas.copy();
    }

    public static void setGas(ItemStack stack, GasStack gas) {
        if (!isBalloon(stack)) {
            return;
        }

        if (gas.isEmpty()) {
            stack.remove(CCBDataComponents.BALLOON_GAS);
            return;
        }

        stack.set(CCBDataComponents.BALLOON_GAS, gas.copy());
    }

    public static float getHookDistance(ItemStack balloon) {
        if (!(balloon.getItem() instanceof BalloonItem balloonItem)) {
            return 1;
        }

        return balloonItem.style.riggingOffset() / 16.0F;
    }

    public static float getBoxDistance(ItemStack balloon) {
        if (!(balloon.getItem() instanceof BalloonItem balloonItem)) {
            return 1;
        }

        return 0.6875F + (balloonItem.style.width() - 12) / 32.0F;
    }

    private static void awardRelease(@Nullable Player player, Level level, ItemStack balloon) {
        if (!(player instanceof ServerPlayer serverPlayer) || level.dimension() != Level.OVERWORLD || !getGas(balloon).is(CCBGases.ETHEREAL_AIR)) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        AdvancementHolder endVisit = server.getAdvancements().get(END_VISIT_ADVANCEMENT);
        if (endVisit == null || !serverPlayer.getAdvancements().getOrStartProgress(endVisit).isDone()) {
            return;
        }

        CCBAdvancements.MINTY_FIREWORKS.awardTo(serverPlayer);
    }

    private static Vec3 placementPoint(UseOnContext context, float height, float radius) {
        Vec3 point = context.getClickLocation();
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) {
            return point.subtract(0, height + 0.25F, 0);
        }

        if (face.getAxis().isHorizontal()) {
            return point.add(Vec3.atLowerCornerOf(face.getNormal()).scale(radius));
        }

        return point;
    }

    @Override
    public String getDescriptionId() {
        if (rare) {
            return "item." + CCBAPI.MOD_ID + ".rare_balloon";
        }

        return "item." + CCBAPI.MOD_ID + ".balloon";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GasStack gas = getGas(stack);
        if (gas.isEmpty()) {
            return;
        }

        tooltip.add(CCBLang.gasName(gas.getGasType()).add(CCBLang.text(" ").add(GasUnitFormat.amount(gas.getAmount()))).style(ChatFormatting.GRAY).component());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null && player.isShiftKeyDown()) {
            return open(level, player, context.getHand()).getResult();
        }

        ItemStack balloon = context.getItemInHand();
        Vec3 clickPoint = context.getClickLocation();
        float scale = BalloonWorldPhysics.of(balloon, level, BlockPos.containing(clickPoint)).linearScale();
        float height = getHeight(balloon) * scale;
        float radius = getWidth(balloon) * scale / 2;
        Vec3 point = placementPoint(context, height, radius);
        float finalScale = BalloonWorldPhysics.of(balloon, level, BlockPos.containing(point)).linearScale();
        if (scale != finalScale) {
            height = getHeight(balloon) * finalScale;
            radius = getWidth(balloon) * finalScale / 2;
            point = placementPoint(context, height, radius);
        }

        AABB scanBox = new AABB(point, point).inflate(radius, 0, radius).expandTowards(0, height, 0);
        if (!level.getEntities(AllEntityTypes.PACKAGE.get(), scanBox, entity -> true).isEmpty()) {
            return InteractionResult.PASS;
        }

        PackageEntity entity = new PackageEntity(level, point.x, point.y, point.z);
        entity.setBox(balloon.copy());
        level.addFreshEntity(entity);
        awardRelease(player, level, balloon);
        balloon.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int ticks) {
        ItemStack released = stack.copy();
        int duration = getUseDuration(stack, entity) - ticks;
        super.releaseUsing(stack, level, entity, ticks);
        if (!(entity instanceof Player player) || duration < 0 || getPackageVelocity(duration) < MIN_RELEASE_VELOCITY) {
            return;
        }

        awardRelease(player, level, released);
    }

    @Override
    public InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (isBalloon(heldStack)) {
            return InteractionResultHolder.fail(heldStack);
        }

        return super.open(level, player, hand);
    }
}
