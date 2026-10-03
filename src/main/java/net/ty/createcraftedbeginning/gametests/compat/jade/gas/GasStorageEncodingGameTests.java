package net.ty.createcraftedbeginning.gametests.compat.jade.gas;

import com.mojang.serialization.DataResult;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.jade.gas.GasStorageEntry;
import net.ty.createcraftedbeginning.compat.jade.gas.GasStorageView;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@PrefixGameTestTemplate(false)
public final class GasStorageEncodingGameTests {
    private GasStorageEncodingGameTests() {
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void validEntriesRetainTheirEncodedData(GameTestHelper helper) {
        DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.MAX_STACK_SIZE, 16).build();
        GasStorageEntry stored = GasStorageEntry.of(CCBGases.NATURAL_AIR.get(), 1234, components);
        for (GasStorageEntry entry : List.of(GasStorageEntry.empty(), stored)) {
            Tag expected = GasStorageEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry).getOrThrow(error -> new IllegalStateException("Failed to encode gas storage test data: " + error));
            CompoundTag ordinary = GasStorageView.writeAmountOnly(entry, false);
            CompoundTag creative = GasStorageView.writeAmountOnly(entry, true);
            Tag ordinaryGas = ordinary.get("gas");
            if (ordinaryGas == null) {
                throw new NullPointerException("Expected the 'gas' tag in the ordinary gas storage payload.");
            }

            Tag creativeGas = creative.get("gas");
            if (creativeGas == null) {
                throw new NullPointerException("Expected the 'gas' tag in the creative gas storage payload.");
            }

            helper.assertValueEqual(ordinaryGas, expected, "ordinary gas entry payload");
            helper.assertValueEqual(creativeGas, expected, "creative gas entry payload");
            helper.assertTrue(!ordinary.contains("creative") && creative.getBoolean("creative"), "Gas storage creative flags changed during encoding.");
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void invalidComponentsRetainTheCodecFailure(GameTestHelper helper) {
        DataComponentPatch components = DataComponentPatch.builder().set(DataComponents.MAX_STACK_SIZE, 0).build();
        GasStorageEntry entry = GasStorageEntry.of(CCBGases.NATURAL_AIR.get(), 1234, components);
        DataResult<Tag> encoded = GasStorageEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry);
        helper.assertTrue(encoded.result().isEmpty(), "Invalid test component unexpectedly encoded successfully.");
        String codecMessage = encoded.error().orElseThrow(() -> new IllegalStateException("Expected an encoding error for the invalid test component.")).message();
        try {
            GasStorageView.writeAmountOnly(entry, false);
        }
        catch (NoSuchElementException exception) {
            String message = exception.getMessage();
            helper.assertTrue(message != null && message.startsWith("Failed to encode Jade gas storage entry: ") && message.endsWith(codecMessage), "Jade encoding failure lost its operation context or original Codec error.");
            helper.succeed();
            return;
        }

        helper.fail("Jade accepted an entry that had no successful Codec result.");
    }
}
