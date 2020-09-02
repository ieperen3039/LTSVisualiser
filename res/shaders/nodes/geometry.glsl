#version 330
//#pragma unroll loops

layout (points) in;
layout (triangle_strip, max_vertices = 35) out;

in vec4[1] geoMiddle;// view position of middle
in vec4[1] geoColor;// color
in int[1] geoID;

smooth out float distanceFromMiddle;
out vec4 fragColor;

vec4 color;

uniform mat4 projectionMatrix;
uniform float nodeRadius;
uniform bool doUniqueColor;

const int nrOfOffsets = 16;
const vec2[] offsets = {
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
};

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
    fragColor = color;;
    EmitVertex();
}

void emitOffset(vec2 offset){
    distanceFromMiddle = 1;
    gl_Position = geoMiddle[0] + projectionMatrix * vec4(offset * nodeRadius, 0.0, 0.0);
    fragColor = color;
    EmitVertex();
}

void main() {
    color = doUniqueColor ? numberToColor(geoID) : geoColor[0];

    for (int i = 0; i < nrOfOffsets; i++){
        emitOffset(offsets[i]);
        emitMiddle();
    }

    emitOffset(offsets[0]);

    EndPrimitive();
}
