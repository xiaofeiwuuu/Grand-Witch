#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    // most of the colour is gone, and what is left leans one way, so that grass and wood come out much alike
    vec3 base = l * vec3(0.93, 1.0, 0.82);
    vec3 col = mix(base, c, 0.22);
    // red and yellow are what it is quick to see: warm, and not leaning to green
    float warm = smoothstep(0.22, 0.55, c.r - c.b) * (1.0 - smoothstep(0.0, 0.2, c.g - c.r));
    col = mix(col, clamp(c * vec3(1.15, 1.0, 0.9), 0.0, 1.0), warm);
    // a cool cast over all of it
    col *= vec3(0.94, 0.98, 1.06);
    // the corners go dark: only the middle of the screen is seen clearly
    float edge = length((texCoord - vec2(0.5)) * vec2(1.0, 0.85)) / 0.7071;
    col *= 1.0 - smoothstep(0.45, 0.95, edge);
    fragColor = vec4(col, 1.0);
}
