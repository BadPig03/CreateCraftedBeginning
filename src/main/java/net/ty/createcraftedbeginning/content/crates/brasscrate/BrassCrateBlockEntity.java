package net.ty.createcraftedbeginning.content.crates.brasscrate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import net.ty.createcraftedbeginning.content.crates.CratesBlockEntity;
import net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BrassCrateBlockEntity extends CratesBlockEntity implements ThresholdSwitchObservable {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final CrateItemStackHandler handler;
    private CCBAdvancementBehaviour advancementBehaviour;
    private FilteringBehaviour filteringBehaviour;

    public BrassCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        handler = new BrassItemHandler(this);
    }

    public static void registerCapabilities(@NotNull RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ItemHandler.BLOCK, CCBBlockEntities.BRASS_CRATE.get(), (be, context) -> be.handler);
    }

    public FilteringBehaviour getFilteringBehaviour() {
        return filteringBehaviour;
    }

    @Override
    public void addBehaviours(@NotNull List<BlockEntityBehaviour> behaviours) {
        filteringBehaviour = new FilteringBehaviour(this, new BrassSmartFilterSlot());
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.BOX_A_HOUSE_OF_GOLD);
        behaviours.add(filteringBehaviour);
        behaviours.add(advancementBehaviour);
    }

    @Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

    @Override
    public CrateItemStackHandler getHandler() {
        return handler;
    }

    @Override
    public void setStoredItems(ItemStack content, int count) {
        handler.setStackInSlot(0, content);
        handler.setCountInSlot(0, count);
        notifyUpdate();
    }

    @Override
    protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        compound.put(COMPOUND_KEY_INVENTORY, handler.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        if (!compound.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        handler.deserializeNBT(registries, compound.getCompound(COMPOUND_KEY_INVENTORY));
    }

    @Override
    public int getMaxValue() {
        return handler.getSlotLimit(0);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getCurrentValue() {
        return handler.getCountInSlot(0);
    }

    @Override
    public MutableComponent format(int value) {
        return CreateLang.text(value + " ").add(CreateLang.translate("schedule.condition.threshold.items")).component();
    }

    private class BrassSmartFilterSlot extends ValueBoxTransform {
        @Contract(value = "_, _, _ -> new", pure = true)
        @Override
        public @NotNull Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return new Vec3(0.5, 0.84375, 0.5);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, @NotNull BlockState state, PoseStack ms) {
            Direction facing = state.getValue(SturdyCrateBlock.FACING);
            TransformStack.of(ms).rotateXDegrees(90).rotateZDegrees(facing.getOpposite().toYRot());
        }
    }

    private class BrassItemHandler extends CrateItemStackHandler {
        BrassItemHandler(@NotNull BrassCrateBlockEntity be) {
            super(CCBConfig.server().crates.maxBrassCapacity.get(), be.filteringBehaviour);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (content.is(Items.GOLD_INGOT) && count >= maxCount) {
                advancementBehaviour.awardPlayer(CCBAdvancements.BOX_A_HOUSE_OF_GOLD);
            }

            notifyUpdate();
        }
    }
}
