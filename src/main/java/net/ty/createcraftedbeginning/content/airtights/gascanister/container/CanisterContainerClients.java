package net.ty.createcraftedbeginning.content.airtights.gascanister.container;

import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.platform.client.ClientContextBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CanisterContainerClients {
    public static final String COMPOUND_KEY_STORED_GAS_TYPE = "CreateCraftedBeginningStoredGasType";

    private static final int BAR_WIDTH = 13;
    private static volatile DisplayedGasState syncedDisplayedGasState = DisplayedGasState.UNSYNCED;

    private CanisterContainerClients() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void updateDisplayedGasState(GasStack content, long capacity, int packType, boolean creative) {
        syncedDisplayedGasState = DisplayedGasState.synced(content, capacity, packType, creative);
    }

    @OnlyIn(Dist.CLIENT)
    public static void clearDisplayedGasState() {
        syncedDisplayedGasState = DisplayedGasState.UNSYNCED;
    }

    @OnlyIn(Dist.CLIENT)
    public static DisplayedGasState getSyncedDisplayedGasState() {
        return syncedDisplayedGasState;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isBarVisible() {
        DisplayedGasState displayedState = getDisplayedGasState();
        return !displayedState.content().isEmpty() && (displayedState.creative() || displayedState.capacity() > 0);
    }

    @OnlyIn(Dist.CLIENT)
    public static int getBarColor() {
        float gasRatio = getDisplayedGasRatio();
        if (gasRatio == 0) {
            return 0;
        }
        return Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, gasRatio);
    }

    @OnlyIn(Dist.CLIENT)
    public static int getBarWidth() {
        float gasRatio = getDisplayedGasRatio();
        return gasRatio == 0 ? 0 : Math.round(BAR_WIDTH * gasRatio);
    }

    @OnlyIn(Dist.CLIENT)
    public static GasStack getDisplayedGasContent() {
        return getDisplayedGasState().content().copy();
    }

    @OnlyIn(Dist.CLIENT)
    private static float getDisplayedGasRatio() {
        DisplayedGasState displayedState = getDisplayedGasState();
        if (displayedState.content().isEmpty()) {
            return 0;
        }

        if (displayedState.creative()) {
            return 1;
        }

        if (displayedState.capacity() <= 0) {
            return 0;
        }
        return Mth.clamp((float) displayedState.content().getAmount() / displayedState.capacity(), 0, 1);
    }

    @OnlyIn(Dist.CLIENT)
    private static DisplayedGasState getDisplayedGasState() {
        Player player = ClientContextBridge.getClientPlayer();
        if (player == null) {
            return DisplayedGasState.EMPTY;
        }

        DisplayedGasState syncedState = syncedDisplayedGasState;
        if (syncedState.synced()) {
            return syncedState;
        }

        var fallbackGasInfo = CanisterContainerSuppliers.getFirstCanisterSupplierPair(player);
        GasStack gasContent = fallbackGasInfo.getFirst();
        if (gasContent.isEmpty()) {
            return DisplayedGasState.EMPTY;
        }
        return DisplayedGasState.fallback(gasContent, fallbackGasInfo.getSecond().getFirst(), fallbackGasInfo.getSecond().getSecond());
    }

    public static int getBarColor(ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long capacity = canisterContents.getTankCapacity(0);
        if (amount == 0 || capacity == 0) {
            return 0;
        }

        float gasRatio = Mth.clamp((float) amount / capacity, 0, 1);
        return Color.mixColors(GasCanisterUtils.COLOR_CYAN, GasCanisterUtils.COLOR_WHITE, gasRatio);
    }

    public static int getBarWidth(ItemStack canister) {
        if (!(canister.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long capacity = canisterContents.getTankCapacity(0);
        if (amount == 0 || capacity == 0) {
            return 0;
        }

        float gasRatio = Mth.clamp((float) amount / capacity, 0, 1);
        return Math.round(BAR_WIDTH * gasRatio);
    }

    public static Gas getStoredGasType(Player player) {
        ResourceLocation gasId = NBTHelper.readResourceLocation(player.getPersistentData(), COMPOUND_KEY_STORED_GAS_TYPE);
        return Gas.getGasTypeByName(gasId);
    }

    public record DisplayedGasState(GasStack content, long capacity, int packType, boolean creative, boolean synced) {
        private static final DisplayedGasState EMPTY = new DisplayedGasState(GasStack.EMPTY, -1, -1, false, false);
        private static final DisplayedGasState UNSYNCED = new DisplayedGasState(GasStack.EMPTY, -1, -1, false, false);

        public DisplayedGasState {
            content = content.copy();
        }

        private static DisplayedGasState synced(GasStack content, long capacity, int packType, boolean creative) {
            return new DisplayedGasState(content, capacity, packType, creative, true);
        }

        private static DisplayedGasState fallback(GasStack content, long capacity, boolean creative) {
            return new DisplayedGasState(content, capacity, -1, creative, false);
        }
    }
}
