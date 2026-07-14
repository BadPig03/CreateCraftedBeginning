package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.box.PackageStyles.PackageStyle;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.data.CCBLang;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BalloonItem extends PackageItem {
    private final boolean rare;

    public BalloonItem(Properties properties, PackageStyle style, boolean rare) {
        super(properties, style);
        this.rare = rare;
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.STANDARD_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
        (rare ? BalloonStyleUtils.RARE_BALLOONS : BalloonStyleUtils.REGULAR_BALLOONS).add(this);
    }

    public boolean isRare() {
        return rare;
    }

    @Override
    public String getDescriptionId() {
        return "item." + CreateCraftedBeginning.MOD_ID + (rare ? ".rare_balloon" : ".balloon");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        BalloonGasContents contents = BalloonUtils.getGasContents(stack);
        if (contents.isEmpty()) {
            return;
        }

        long capacity = BalloonUtils.getCapacity();
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        tooltip.add(CCBLang.translate("gui.tooltips.balloon.content").style(ChatFormatting.GRAY).component());
        for (GasStack gas : contents.gases()) {
            Component amount = CCBLang.number(gas.getAmount()).add(CCBLang.translate("gui.goggles.unit.milli_buckets")).component();
            Component line = Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY).append(gas.getHoverName().copy().withStyle(ChatFormatting.GOLD)).append(Component.literal(": ").withStyle(ChatFormatting.GRAY)).append(amount.copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(line);
            if (flag.isAdvanced()) {
                tooltip.add(Component.literal("    " + gas.getGasType().getResourceLocation()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        long displayedAmount = Math.clamp(contents.totalAmount(), 0, Math.max(0, capacity));
        tooltip.add(CCBLang.translate("gui.tooltips.balloon.capacity").add(CCBLang.number(displayedAmount).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
    }

    @Override
    public InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (BalloonUtils.isBalloon(stack)) {
            return InteractionResultHolder.fail(stack);
        }

        return super.open(level, player, hand);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return BalloonUtils.containsGasContents(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        BalloonGasContents contents = BalloonUtils.getGasContents(stack);
        long capacity = BalloonUtils.getCapacity();
        if (contents.isEmpty() || capacity <= 0) {
            return 0;
        }

        return Mth.clamp((int) Math.round(13 * contents.totalAmount() / (double) capacity), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BalloonUtils.getDisplayColor(BalloonUtils.getGasContents(stack));
    }
}
