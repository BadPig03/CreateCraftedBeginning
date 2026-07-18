package net.ty.createcraftedbeginning.mixin.client.create;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelEffectPacket;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FactoryPanelEffectPacket.class, remap = false)
public abstract class FactoryPanelEffectPacketMixin {
    @Shadow
    public abstract FactoryPanelPosition fromPos();
    @Shadow
    public abstract FactoryPanelPosition toPos();
    @Shadow
    public abstract boolean success();

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void ccb$handle(LocalPlayer player, CallbackInfo ci) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(fromPos().pos());
        if (!(state.getBlock() instanceof GasFactoryGaugeBlock)) {
            return;
        }

        FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(level, toPos());
        if (behaviour == null) {
            ci.cancel();
            return;
        }

        behaviour.bulb.setValue(1);
        FactoryPanelConnection connection = behaviour.targetedBy.get(fromPos());
        if (connection != null) {
            connection.success = success();
        }
        ci.cancel();
    }
}
