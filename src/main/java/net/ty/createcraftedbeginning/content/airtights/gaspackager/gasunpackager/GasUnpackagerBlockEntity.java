package net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.CapManipulationBehaviourBase.InterfaceProvider;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.compat.computercraft.ComputerCraftPackagerCompat;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPressureSemantics;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerPendingGas.InsertionResult;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasManipulationBehaviour;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBGasTags;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasUnpackagerBlockEntity extends PackagerBlockEntity implements Clearable {
    private final GasUnpackagerPendingGas pendingGas;
    private GasManipulationBehaviour gasInventory;

    public GasUnpackagerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        pendingGas = new GasUnpackagerPendingGas();
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.GAS_UNPACKAGER.get(), (unpackager, context) -> unpackager.inventory);
    }

    private static boolean isForeignGas(Level level, GasStack gas) {
        if (gas.isEmpty()) {
            return false;
        }

        ResourceKey<Level> dimension = level.dimension();
        if (gas.is(CCBGasTags.ETHEREAL.tag)) {
            return dimension != Level.END;
        }

        if (gas.is(CCBGasTags.ULTRAWARM.tag)) {
            return dimension != Level.NETHER;
        }

        return (gas.is(CCBGasTags.NATURAL.tag) || gas.is(CCBGasTags.MOIST.tag) || gas.is(CCBGasTags.SPORE.tag) || gas.is(CCBGasTags.SCULK.tag)) && dimension != Level.OVERWORLD;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        gasInventory = new GasManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(target -> target != null && !(target instanceof PortableGasInterfaceBlockEntity));
        behaviours.add(gasInventory);

        targetInventory = new InvManipulationBehaviour(this, InterfaceProvider.oppositeOfBlockFacing()).withFilter(target -> target != null && !(target instanceof PortableStorageInterfaceBlockEntity));
        behaviours.add(targetInventory);

        behaviours.add(new CCBAdvancementBehaviour(this, CCBAdvancements.A_GUIDE_TO_OTHERWORLDLY_LOGISTICS));
        ComputerCraftPackagerCompat.addBehaviour(this, behaviours);
    }

    @Override
    public void tick() {
        boolean shouldInsertGas = level != null && !level.isClientSide() && animationInward && animationTicks == 1 && !pendingGas.isEmpty();
        super.tick();
        if (!shouldInsertGas) {
            return;
        }

        performPendingGasInsertion();
        setChanged();
    }

    @Override
    public InventorySummary getAvailableItems() {
        return new InventorySummary();
    }

    @Override
    public void lazyTick() {
        redstonePowered = false;
        super.lazyTick();
    }

    @Override
    public void recheckIfLinksPresent() {
    }

    @Override
    public boolean redstoneModeActive() {
        return false;
    }

    @Override
    public void activate() {
    }

    @Override
    public boolean unwrapBox(ItemStack box, boolean simulate) {
        if (animationTicks > 0 || !BalloonItem.containsGas(box) || level == null) {
            return false;
        }

        long ambientPressurePa = BalloonPressureSemantics.ambientPressurePa(level, worldPosition);
        GasHandler gasHandler = gasHandler();
        if (gasHandler == null || !pendingGas.canStage(box, gasHandler, ambientPressurePa)) {
            return false;
        }

        if (simulate) {
            return true;
        }

        pendingGas.stage(box);
        previouslyUnwrapped = box.copyWithCount(1);
        animationInward = true;
        animationTicks = CYCLE;
        ComputerCraftPackagerCompat.emitPackageReceived(this, box);
        notifyUpdate();
        return true;
    }

    @Override
    public void attemptToSend(@Nullable List<PackagingRequest> queuedRequests) {
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        redstonePowered = false;
        pendingGas.read(compoundTag, provider, clientPacket);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        pendingGas.write(compoundTag, provider, clientPacket);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        pendingGas.clear();
    }

    @Override
    public void destroy() {
        if (level != null && !level.isClientSide() && !pendingGas.isEmpty() && !previouslyUnwrapped.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), previouslyUnwrapped.copy());
        }
        pendingGas.clear();
        super.destroy();
    }

    void targetChanged(BlockPos neighborPos) {
        if (gasInventory != null) {
            gasInventory.onNeighborChanged(neighborPos);
        }
        if (targetInventory == null) {
            return;
        }

        targetInventory.onNeighborChanged(neighborPos);
    }

    private void performPendingGasInsertion() {
        long ambientPressurePa = level == null ? GasPressure.VACUUM_PA : BalloonPressureSemantics.ambientPressurePa(level, worldPosition);
        InsertionResult insertionResult = pendingGas.insertInto(gasHandler(), previouslyUnwrapped, ambientPressurePa);
        if (!insertionResult.returnedBalloon().isEmpty()) {
            queuedExitingPackages.addFirst(new BigItemStack(insertionResult.returnedBalloon(), 1));
        }
        if (insertionResult.transferredAmount() > 0 && level != null && isForeignGas(level, BalloonItem.getGas(previouslyUnwrapped))) {
            getBehaviour(CCBAdvancementBehaviour.TYPE).awardPlayer(CCBAdvancements.A_GUIDE_TO_OTHERWORLDLY_LOGISTICS);
        }
        pendingGas.clear();
        notifyUpdate();
    }

    @Nullable
    private GasHandler gasHandler() {
        if (gasInventory == null) {
            return null;
        }

        return gasInventory.getInventory();
    }
}
