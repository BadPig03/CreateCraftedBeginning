package net.ty.createcraftedbeginning.gametests.gas;

import com.google.gson.GsonBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler;
import net.ty.createcraftedbeginning.gas.network.solver.GasSolverProfiler.NetworkProfile;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasNetworkBenchmarkSupport {
    static final boolean ENABLED = Boolean.getBoolean("createcraftedbeginning.gas_network_benchmark");
    static final int WARMUP_TICKS = Math.clamp(Integer.getInteger("createcraftedbeginning.gas_benchmark_warmup", 60), 20, 200);
    static final int SAMPLE_TICKS = Math.clamp(Integer.getInteger("createcraftedbeginning.gas_benchmark_samples", 100), 40, 400);

    private GasNetworkBenchmarkSupport() {}

    static void report(String label, List<NetworkProfile> samples) {
        GasSolverProfiler.logSummary(label, samples);
        double allocation = samples.stream().mapToLong(profile -> profile.diagnostics().allocatedBytes()).filter(allocatedBytes -> allocatedBytes >= 0).average().orElse(-1);
        long regularized = samples.stream().mapToLong(profile -> profile.diagnostics().regularizedSolves()).sum();
        long iterations = samples.stream().mapToLong(profile -> profile.diagnostics().regularizedIterations()).sum();
        CCBAPI.LOGGER.info("Gas benchmark [{}]: allocationBytesPerSample={}, regularizedSolvesTotal={}, regularizedIterationsTotal={}", label, allocation, regularized, iterations);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", 1);
        result.put("label", label);
        result.put("warmupTicks", WARMUP_TICKS);
        result.put("sampleTicks", SAMPLE_TICKS);
        result.put("java", System.getProperty("java.runtime.version"));
        result.put("vm", System.getProperty("java.vm.name"));
        result.put("jvmArgs", ManagementFactory.getRuntimeMXBean().getInputArguments());
        result.put("os", System.getProperty("os.name") + ' ' + System.getProperty("os.arch"));
        result.put("processors", Runtime.getRuntime().availableProcessors());
        result.put("summary", GasSolverProfiler.summarize(samples));
        result.put("samples", samples);
        Path directory = Path.of(System.getProperty("createcraftedbeginning.gas_benchmark_output", "gas-network-benchmark"));
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(label + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }
        catch (IOException failure) {
            throw new IllegalStateException("Failed to export gas benchmark '" + label + "' to '" + directory.resolve(label + ".json") + "'.", failure);
        }
    }
}
