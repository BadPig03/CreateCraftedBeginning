#version 150

uniform sampler2D CloudsSampler;
uniform sampler2D ForegroundSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 cloud = texture(CloudsSampler, texCoord);
    vec4 foreground = texture(ForegroundSampler, texCoord);
    fragColor = vec4(cloud.rgb * (1.0 - foreground.a) + foreground.rgb * cloud.a, cloud.a);
}
