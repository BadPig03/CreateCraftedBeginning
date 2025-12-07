package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class CratesBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public CratesBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean addToGoggleTooltip(@NotNull List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.goggles.crates.header").forGoggles(tooltip);
        CrateItemStackHandler handler = getHandler();
        ItemStack content = handler.getStackInSlot(0);
        int count = handler.getCountInSlot(0);
        int maxCount = handler.getSlotLimit(0);
        if (content.isEmpty() || count == 0) {
            CCBLang.translate("gui.goggles.crates.capacity").style(ChatFormatting.GRAY).add(CCBLang.number(maxCount).style(ChatFormatting.GOLD)).forGoggles(tooltip, 1);
        }
        else {
            CCBLang.text(Component.translatable(content.getDescriptionId()).getString()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
            CCBLang.number(count).style(ChatFormatting.GOLD).add(CCBLang.text(" / ").style(ChatFormatting.GRAY)).add(CCBLang.number(maxCount).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        }
        return true;
    }

    public abstract CrateItemStackHandler getHandler();

    public abstract void setStoredItems(ItemStack content, int count);

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }
}
