package net.ty.createcraftedbeginning.platform;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.platform.access.BasinTransactionAccess;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BasinTransactionBridge {
    private BasinTransactionBridge() {
    }

    public static @Nullable TransactionHandle createHandle(BasinBlockEntity basin) {
        if (!(basin instanceof BasinTransactionAccess access)) {
            return null;
        }
        return new TransactionHandle(access);
    }

    public static final class TransactionHandle {
        private final BasinTransactionAccess access;

        private TransactionHandle(BasinTransactionAccess access) {
            this.access = access;
        }

        public List<FluidStack> snapshotFluidOverflow() {
            return access.ccb$copyTransactionFluidOverflow();
        }

        public void restoreFluidOverflow(List<FluidStack> snapshot) {
            access.ccb$restoreTransactionFluidOverflow(snapshot);
        }
    }
}
