#version 330
//#pragma unroll loops

layout (points) in;
layout (triangle_strip, max_vertices = 35) out;

in vec4[1] geoMiddle;// viewProjection position of middle
in vec4[1] geoColor;
in vec4[1] geoBorder;
in int[1] geoID;

smooth out float distanceFromMiddle;
out vec4 fragColor;
out vec4 fragBorder;

uniform mat4 projectionMatrix;
uniform float nodeRadius;
uniform bool doUniqueColor;

const int nrOfOffsets = 16;
const vec2[] offsets = vec2[](
vec2(0.000, 1.000),
vec2(0.383, 0.924),
vec2(0.707, 0.707),
vec2(0.924, 0.383),
vec2(1.000, 0.000),
vec2(0.924, -0.383),
vec2(0.707, -0.707),
vec2(0.383, -0.924),
vec2(0.000, -1.000),
vec2(-0.383, -0.924),
vec2(-0.707, -0.707),
vec2(-0.924, -0.383),
vec2(-1.000, -0.000),
vec2(-0.924, 0.383),
vec2(-0.707, 0.707),
vec2(-0.383, 0.924)
);

vec4 color;
vec4 border;

vec4 numberToColor(int i) {
    int bitSize = (1 << 6);
    int r = (i % bitSize) << 2;
    int g = (((i >> 6) % bitSize) << 2);
    int b = (((i >> 12) % bitSize) << 2);

    return vec4(r / 255.0, g / 255.0, b / 255.0, 1.0);
}

void emitMiddle(){
    distanceFromMiddle = 0;
    gl_Position = geoMiddle[0];
    fragColor = color;
    fragBorder = border;
    EmitVertex();
}

void emitOffset(vec2 offset){
    distanceFromMiddle = 1;
    gl_Position = geoMiddle[0] + projectionMatrix * vec4(offset * nodeRadius, 0.0, 0.0);
    fragColor = color;
    fragBorder = border;
    EmitVertex();
}

void main() {
    if (doUniqueColor){
        color = numberToColor(geoID[0]);
        border = color;

    } else {
        color = geoColor[0];
        border = geoBorder[0];
    }

    for (int i = 0; i < nrOfOffsets; i++){
        emitOffset(offsets[i]);
        emitMiddle();
    }

    emitOffset(offsets[0]);

    EndPrimitive();
}
