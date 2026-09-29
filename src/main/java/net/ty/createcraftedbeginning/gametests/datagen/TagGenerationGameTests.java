package net.ty.createcraftedbeginning.gametests.datagen;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider.IntrinsicImpl;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.registrate.CCBRegistrateProvider;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TagGenerationGameTests {
    private TagGenerationGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void dependencyAndOutputShareGeneration(GameTestHelper helper) {
        CompletableFuture<Provider> lookup = new CompletableFuture<>();
        CountingTags first = new CountingTags(CCBRegistrateProvider.get(), lookup, null);
        CountingTags second = new CountingTags(CCBRegistrateProvider.get(), lookup, null);
        CompletableFuture<Provider> dependency = first.getFilledProvider();
        CompletableFuture<?> output = first.run(CachedOutput.NO_CACHE);
        CompletableFuture<Provider> otherContents = second.getFilledProvider();
        lookup.complete(helper.getLevel().registryAccess());
        CompletableFuture.allOf(dependency, output, otherContents).join();
        helper.assertValueEqual(first.generations.get(), 1, "shared dependency and output generation");
        helper.assertValueEqual(second.generations.get(), 1, "independent provider generation");
        helper.assertTrue(dependency == first.getFilledProvider(), "Completed generation must remain shared");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void otherModsKeepTheirGenerationLifecycle(GameTestHelper helper) {
        CompletableFuture<Provider> lookup = new CompletableFuture<>();
        CountingTags provider = new CountingTags(CreateRegistrate.create("ccb_generation_probe"), lookup, null);
        CompletableFuture<Provider> dependency = provider.getFilledProvider();
        CompletableFuture<?> output = provider.run(CachedOutput.NO_CACHE);
        lookup.complete(helper.getLevel().registryAccess());
        CompletableFuture.allOf(dependency, output).join();
        helper.assertValueEqual(provider.generations.get(), 2, "external provider lifecycle");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void generationFailureReachesBothConsumers(GameTestHelper helper) {
        CompletableFuture<Provider> lookup = new CompletableFuture<>();
        RuntimeException failure = new IllegalStateException("Intentional tag generation failure.");
        CountingTags provider = new CountingTags(CCBRegistrateProvider.get(), lookup, failure);
        CompletableFuture<Provider> dependency = provider.getFilledProvider();
        CompletableFuture<?> output = provider.run(CachedOutput.NO_CACHE);
        lookup.complete(helper.getLevel().registryAccess());
        for (CompletableFuture<?> result : new CompletableFuture<?>[]{dependency, output}) {
            Throwable cause = result.handle((value, exception) -> exception).join();
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            helper.assertTrue(cause == failure, "Generation failure must reach every consumer without being replaced by success");
        }
        helper.assertValueEqual(provider.generations.get(), 1, "failed generation count");
        helper.assertTrue(dependency == provider.getFilledProvider(), "Failed generation must remain shared");
        helper.succeed();
    }

    private static final class CountingTags extends IntrinsicImpl<Block> {
        private final AtomicInteger generations = new AtomicInteger();
        private final @Nullable RuntimeException failure;

        @SuppressWarnings({"deprecation", "DataFlowIssue"})
        private CountingTags(CreateRegistrate owner, CompletableFuture<Provider> lookup, @Nullable RuntimeException failure) {
            super(owner, ProviderType.BLOCK_TAGS, "generation_test", new PackOutput(Path.of("build", "tag-generation-tests")), Registries.BLOCK, lookup, block -> block.builtInRegistryHolder().key(), null);
            this.failure = failure;
        }

        @Override
        protected void addTags(Provider provider) {
            generations.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
        }
    }
}
