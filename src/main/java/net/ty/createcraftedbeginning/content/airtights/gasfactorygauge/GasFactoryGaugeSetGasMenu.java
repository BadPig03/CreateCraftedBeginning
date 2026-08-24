package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.gui.menu.GhostItemMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeSetGasMenu extends GhostItemMenu<GasFactoryGaugeBehaviour> {
    public GasFactoryGaugeSetGasMenu(int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.GAS_FACTORY_GAUGE_SET_GAS_MENU.get(), id, inventory, extraData);
    }

    private GasFactoryGaugeSetGasMenu(MenuType<?> type, int id, Inventory inventory, GasFactoryGaugeBehaviour contentHolder) {
        super(type, id, inventory, contentHolder);
    }

    private GasFactoryGaugeSetGasMenu(MenuType<?> type, int id, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        super(type, id, inventory, extraData);
    }

    @Contract("_, _, _ -> new")
    static GasFactoryGaugeSetGasMenu create(int id, Inventory inventory, GasFactoryGaugeBehaviour behaviour) {
        return new GasFactoryGaugeSetGasMenu(CCBMenuTypes.GAS_FACTORY_GAUGE_SET_GAS_MENU.get(), id, inventory, behaviour);
    }

    @Override
    protected ItemStackHandler createGhostInventory() {
        return new ItemStackHandler(1);
    }

    @Override
    protected boolean allowRepeats() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected @Nullable GasFactoryGaugeBehaviour createOnClient(RegistryFriendlyByteBuf extraData) {
        return CCBClientBridge.createGasFactoryGaugeBehaviour(extraData) instanceof GasFactoryGaugeBehaviour behaviour ? behaviour : null;
    }

    @Override
    protected void addSlots() {
        addPlayerSlots(13, 112);
        addSlot(new SlotItemHandler(ghostInventory, 0, 86, 28));
    }

    @Override
    protected void saveData(GasFactoryGaugeBehaviour behaviour) {
        ItemStack gasSource = ghostInventory.getStackInSlot(0);
        if (gasSource.isEmpty()) {
            behaviour.setFilter(ItemStack.EMPTY);
            return;
        }

        List<ItemStack> gasTokens = GasVirtualUtils.getVirtualItems(gasSource);
        if (gasTokens.size() != 1) {
            if (gasTokens.isEmpty()) {
                player.displayClientMessage(CCBLang.translateDirect("gui.warnings.empty_gas_source", gasSource.getHoverName()).withStyle(ChatFormatting.RED), true);
            }
            else {
                player.displayClientMessage(CCBLang.translateDirect("gui.warnings.requires_single_gas").withStyle(ChatFormatting.RED), true);
            }
            AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1, 1);
            return;
        }

        behaviour.setFilter(gasTokens.getFirst());
        player.level().playSound(null, behaviour.getPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25f, 0.1f);
    }
}
