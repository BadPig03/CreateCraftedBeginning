package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BoilerSteamOutletBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int LAZY_TICK_RATE = 20;

    private final BoilerSteamOutletController controller;

    private SmartGasTankBehaviour steamTank;
    private IGasHandler exposedGasHandler;

    public BoilerSteamOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        controller = new BoilerSteamOutletController(this);
        setLazyTickRate(LAZY_TICK_RATE);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(GasHandler.BLOCK, CCBBlockEntities.BOILER_STEAM_OUTLET.get(), (be, direction) -> {
            if (direction != BoilerSteamOutletBlock.getFacing(be.getBlockState())) {
                return null;
            }
            return be.exposedGasHandler;
        });
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        steamTank = new SmartGasTankBehaviour(SmartGasTankBehaviour.OUTPUT, this, 1, BoilerSteamOutletProduction.getMaximumOutputCapacity(), false).forbidInsertion().allowExtraction();
        steamTank.getPrimaryHandler().setValidator(stack -> !stack.isEmpty() && stack.is(CCBGases.STEAM));
        exposedGasHandler = new SteamOutletGasHandler(this, steamTank.getCapability());
        behaviours.add(steamTank);
    }

    @Override
    public void tick() {
        super.tick();
        controller.tickServer();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        controller.lazyTickServer();
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        controller.write(compoundTag, clientPacket);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        controller.read(compoundTag, clientPacket);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CCBLang.translate("gui.boiler_steam_outlet.header").forGoggles(tooltip);
        CCBLang.translate("gui.boiler_steam_outlet.steam_generation").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(controller.getSteamGenerationRate()).space().translate("gui.unit.milli_buckets_per_second").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        CCBLang.translate("gui.boiler_steam_outlet.steam_output").style(ChatFormatting.GRAY).forGoggles(tooltip);
        CCBLang.number(controller.getSteamOutputRate()).space().translate("gui.unit.milli_buckets_per_second").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        return true;
    }

    public void recordExtraction(GasStack drained, GasAction action) {
        controller.recordExtraction(drained, action);
    }

    public void ensureCurrentTick() {
        controller.ensureCurrentTick();
    }

    @Nullable SmartGasTankBehaviour getSteamTankBehaviour() {
        return steamTank;
    }
}
