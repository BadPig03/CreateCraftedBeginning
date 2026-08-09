package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlockEntity;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerStructuralBlockEntity.BlowerWorkingMode;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerBlockEntity extends EndMechanicalBlockEntity<EndIncinerationBlowerStructuralBlockEntity> {
    private static final String COMPOUND_KEY_SHOW_OUTLINE = "ShowOutline";
    private static Consumer<EndIncinerationBlowerBlockEntity> clientTicker = blower -> {};

    private final EndIncinerationBlowerEffectProcessor effectProcessor;
    private final EndIncinerationBlowerOwner ownerState;
    private final EndIncinerationBlowerVisualState visualState;
    private boolean showOutline;

    public EndIncinerationBlowerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        effectProcessor = new EndIncinerationBlowerEffectProcessor(this);
        ownerState = new EndIncinerationBlowerOwner();
        visualState = new EndIncinerationBlowerVisualState();
        showOutline = true;
    }

    public static void setClientTicker(Consumer<EndIncinerationBlowerBlockEntity> ticker) {
        clientTicker = ticker;
    }

    public static float calculateRange(float speed) {
        return EndIncinerationBlowerRange.calculateRange(speed);
    }

    public static AABB calculateArea(BlockPos pos, float speed) {
        return EndIncinerationBlowerRange.calculateArea(pos, speed);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        advancementBehaviour = new CCBAdvancementBehaviour(this, CCBAdvancements.HOT_HOT_HOT, CCBAdvancements.WARM_HEARTED);
        behaviours.add(advancementBehaviour);
    }

    @Override
    protected Class<EndIncinerationBlowerStructuralBlockEntity> getStructuralClass() {
        return EndIncinerationBlowerStructuralBlockEntity.class;
    }

    @Override
    public void updateStructural() {
        convertCasingToStructural(CCBBlocks.END_INCINERATION_BLOWER_STRUCTURAL_BLOCK.getDefaultState());
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }

        if (level.isClientSide) {
            clientTicker.accept(this);
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        effectProcessor.tick(serverLevel);
    }

    @Override
    protected void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.write(compoundTag, provider, clientPacket);
        compoundTag.putBoolean(COMPOUND_KEY_SHOW_OUTLINE, showOutline);
        ownerState.write(compoundTag);
    }

    @Override
    protected void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        super.read(compoundTag, provider, clientPacket);
        if (compoundTag.contains(COMPOUND_KEY_SHOW_OUTLINE)) {
            showOutline = compoundTag.getBoolean(COMPOUND_KEY_SHOW_OUTLINE);
        }
        ownerState.read(compoundTag);
    }

    public void toggleShowOutline() {
        showOutline = !showOutline;
        notifyUpdate();
    }

    public boolean isShowingOutline() {
        return showOutline;
    }

    public void setOwner(UUID owner) {
        if (!ownerState.setOwner(owner)) {
            return;
        }

        setChanged();
    }

    @Nullable EndIncinerationBlowerStructuralBlockEntity getStructuralForEffect() {
        return getStructuralForUse();
    }

    FakePlayer getFakePlayer(ServerLevel level) {
        return ownerState.getFakePlayer(level, worldPosition);
    }

    void awardPrimaryEffectAdvancement() {
        advancementBehaviour.awardPlayer(CCBAdvancements.HOT_HOT_HOT);
    }

    void awardWarmHeartedAdvancement() {
        advancementBehaviour.awardPlayer(CCBAdvancements.WARM_HEARTED);
    }

    void tickClientParticles() {
        if (level == null || !level.isClientSide) {
            return;
        }

        visualState.tick(level, worldPosition, getSpeed(), this::getWorkingModeForVisuals);
    }

    private @Nullable BlowerWorkingMode getWorkingModeForVisuals() {
        EndIncinerationBlowerStructuralBlockEntity structural = getStructuralForUse();
        return structural == null ? null : structural.getBlowerWorkingMode().get();
    }
}
