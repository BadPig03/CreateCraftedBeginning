package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBOpticalPower extends ConfigBase {
    @SuppressWarnings("unused")
    public final ConfigGroup network = group(0, "network", "Optical Power Network");
    public final ConfigInt maxNetworkPowerSu = i(32768, 1024, 16777216, "max_network_power_su", Comments.maxNetworkPowerSu);

    @SuppressWarnings("unused")
    public final ConfigGroup solarCollector = group(0, "solar_collector", "Solar Collector");
    public final ConfigFloat rainOutputMultiplier = f(0.5f, 0, 1, "rain_output_multiplier", Comments.rainOutputMultiplier);

    @SuppressWarnings("unused")
    public final ConfigGroup laserReceiver = group(0, "laser_receiver", "Laser Receiver");
    public final ConfigInt maxLaserReceiverPowerSu = i(8192, 256, 4194304, "max_received_power_su", Comments.maxLaserReceiverPowerSu);

    @Override
    public String getName() {
        return "optical_power";
    }

    private static class Comments {
        private static final String maxNetworkPowerSu = "Maximum Optical Power capacity, expressed in equivalent Create SU, that one Optical Fiber network may distribute at once. Values are rounded down to 256 SU steps.";
        private static final String rainOutputMultiplier = "The Optical Power output multiplier applied to Solar Collectors while rain reaches the collector.";
        private static final String maxLaserReceiverPowerSu = "Maximum combined Optical Power, expressed in equivalent Create SU, accepted by one Laser Receiver. Multiple beams add together up to this limit; values are rounded down to 256 SU steps.";
    }
}
