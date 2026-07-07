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

    @Override
    public String getDescriptionId() {
        return "item." + CreateCraftedBeginning.MOD_ID + (rare ? ".rare_balloon" : ".balloon");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        GasStack gas = BalloonUtils.getGasContents(stack);
        if (gas.isEmpty()) {
            return;
        }

        long capacity = BalloonUtils.getCapacity();
        LangBuilder mb = CCBLang.translate("gui.goggles.unit.milli_buckets");
        tooltip.add(CCBLang.translate("gui.tooltips.balloon.content").add(CCBLang.gasName(gas).style(ChatFormatting.GOLD)).style(ChatFormatting.GRAY).component());
        tooltip.add(CCBLang.translate("gui.tooltips.balloon.capacity").add(CCBLang.number(Mth.clamp(gas.getAmount(), 0, capacity)).add(mb).style(ChatFormatting.GOLD).text(ChatFormatting.GRAY, " / ").add(CCBLang.number(capacity).add(mb).style(ChatFormatting.DARK_GRAY))).style(ChatFormatting.GRAY).component());
    }

    @Override
    public InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (BalloonUtils.isBalloon(stack)) {
            return InteractionResultHolder.fail(stack);
        }

        return super.open(level, player, hand);
    }
}
