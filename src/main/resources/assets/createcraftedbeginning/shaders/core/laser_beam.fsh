#version 150

#moj_import <fog.glsl>

in float vertexDistance;
in vec4 vertexColor;
in vec2 beamBrightness;

uniform vec4 ColorModulator;
uniform float MinWidth;
uniform float ReferenceRadius;
uniform float FogStart;
uniform float FogEnd;
uniform int FogEnabled;
uniform int CloudForeground;
uniform sampler2D CloudsSampler;
uniform sampler2D CloudsDepthSampler;

out vec4 fragColor;

void main() {
    if (CloudForeground != 0) {
        ivec2 pixel = ivec2(gl_FragCoord.xy);
        if (texelFetch(CloudsSampler, pixel, 0).a == 0.0 || gl_FragCoord.z >= texelFetch(CloudsDepthSampler, pixel, 0).r) {
            discard;
        }
    }

    float radius = max(MinWidth, abs(beamBrightness.x)) * 0.5;
    float opacity = min(1.0, beamBrightness.y * ReferenceRadius / radius);
    fragColor = vertexColor * ColorModulator;
    fragColor.a *= opacity;
    if (FogEnabled != 0) {
        fragColor *= linear_fog_fade(vertexDistance, FogStart, FogEnd);
    }
}
