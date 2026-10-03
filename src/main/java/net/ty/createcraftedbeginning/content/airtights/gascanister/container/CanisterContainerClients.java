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
import net.ty.createcraftedbeginning.api.canister.CanisterCapabilities;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.CanisterDisplayColors;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers.CanisterSupplierSnapshot;
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
    public static void updateDisplayedGasState(GasStack content, long maxAmount, long pressurePa, int packType, boolean creative) {
        syncedDisplayedGasState = DisplayedGasState.synced(content, maxAmount, pressurePa, packType, creative);
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
        return !displayedState.content().isEmpty() && (displayedState.creative() || displayedState.maxAmount() > 0);
    }

    @OnlyIn(Dist.CLIENT)
    public static int getBarColor() {
        float gasRatio = getDisplayedGasRatio();
        if (gasRatio == 0) {
            return 0;
        }

        return Color.mixColors(CanisterDisplayColors.COLOR_CYAN, CanisterDisplayColors.COLOR_WHITE, gasRatio);
    }

    public static int getBarColor(ItemStack canister) {
        if (!(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long maxAmount = canisterContents.getTankMaxAmount(0);
        if (amount == 0 || maxAmount == 0) {
            return 0;
        }

        float gasRatio = Mth.clamp((float) amount / maxAmount, 0.0F, 1.0F);
        return Color.mixColors(CanisterDisplayColors.COLOR_CYAN, CanisterDisplayColors.COLOR_WHITE, gasRatio);
    }

    @OnlyIn(Dist.CLIENT)
    public static int getBarWidth() {
        float gasRatio = getDisplayedGasRatio();
        if (gasRatio == 0) {
            return 0;
        }

        return Math.round(BAR_WIDTH * gasRatio);
    }

    public static int getBarWidth(ItemStack canister) {
        if (!(canister.getCapability(CanisterCapabilities.ITEM) instanceof GasCanisterContainerContents canisterContents)) {
            return 0;
        }

        long amount = canisterContents.getGasInTank(0).getAmount();
        long maxAmount = canisterContents.getTankMaxAmount(0);
        if (amount == 0 || maxAmount == 0) {
            return 0;
        }

        float gasRatio = Mth.clamp((float) amount / maxAmount, 0.0F, 1.0F);
        return Math.round(BAR_WIDTH * gasRatio);
    }

    @OnlyIn(Dist.CLIENT)
    public static GasStack getDisplayedGasContent() {
        return getDisplayedGasState().content().copy();
    }

    @OnlyIn(Dist.CLIENT)
    public static long getDisplayedGasPressurePa() {
        return getDisplayedGasState().pressurePa();
    }

    public static Gas getStoredGasType(Player player) {
        ResourceLocation gasId = NBTHelper.readResourceLocation(player.getPersistentData(), COMPOUND_KEY_STORED_GAS_TYPE);
        Gas gasType = Gas.findById(gasId);
        if (!gasType.isUsableInEquipment()) {
            return Gas.EMPTY_GAS_HOLDER.value();
        }

        return gasType;
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

        if (displayedState.maxAmount() <= 0) {
            return 0;
        }

        return Mth.clamp((float) displayedState.content().getAmount() / displayedState.maxAmount(), 0.0F, 1.0F);
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

        CanisterSupplierSnapshot fallbackGasInfo = CanisterContainerSuppliers.getFirstCanisterSupplierSnapshot(player);
        GasStack gasContent = fallbackGasInfo.content();
        if (gasContent.isEmpty()) {
            return DisplayedGasState.EMPTY;
        }

        return DisplayedGasState.fallback(gasContent, fallbackGasInfo.maxAmount(), fallbackGasInfo.pressurePa(), fallbackGasInfo.creative());
    }

    public record DisplayedGasState(GasStack content, long maxAmount, long pressurePa, int packType, boolean creative, boolean synced) {
        private static final DisplayedGasState EMPTY = new DisplayedGasState(GasStack.EMPTY, -1, 0, -1, false, false);
        private static final DisplayedGasState UNSYNCED = new DisplayedGasState(GasStack.EMPTY, -1, 0, -1, false, false);

        public DisplayedGasState {
            if (!content.getGasType().isUsableInEquipment()) {
                content = GasStack.EMPTY;
                maxAmount = -1;
                pressurePa = 0;
                packType = -1;
                creative = false;
            }
            content = content.copy();
            pressurePa = Math.max(0, pressurePa);
        }

        private static DisplayedGasState synced(GasStack content, long maxAmount, long pressurePa, int packType, boolean creative) {
            return new DisplayedGasState(content, maxAmount, pressurePa, packType, creative, true);
        }

        private static DisplayedGasState fallback(GasStack content, long maxAmount, long pressurePa, boolean creative) {
            return new DisplayedGasState(content, maxAmount, pressurePa, -1, creative, false);
        }
    }
}
