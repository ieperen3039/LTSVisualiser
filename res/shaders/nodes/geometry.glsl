#version 330
//#pragma unroll loops

layout (points) in;
layout (triangle_strip, max_vertices = 35) out;

in vec4[1] geoMiddle; // view position of middle
in vec4[1] geoColor; // color

smooth out float distanceFromMiddle;
out vec4 fragColor;

uniform mat4 projectionMatrix;
uniform float nodeRadius;

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
const int nrOfOffsets = 16;// array.length doesn't work

void emitMiddle(){
    distanceFromMiddle = 0;
    gl_Position = geoMiddle[0];
    fragColor = geoColor[0];
    EmitVertex();
}

void emitOffset(vec2 offset){
    distanceFromMiddle = 1;
    gl_Position = geoMiddle[0] + projectionMatrix * vec4(offset * nodeRadius, 0.0, 0.0);
    fragColor = geoColor[0];
    EmitVertex();
}

void main() {
    for (int i = 0; i < nrOfOffsets; i++){
        emitOffset(offsets[i]);
        emitMiddle();
    }

    emitOffset(offsets[0]);

    EndPrimitive();
}
