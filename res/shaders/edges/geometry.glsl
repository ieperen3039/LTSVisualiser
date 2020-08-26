#version 330


layout (points) in;
layout (triangle_strip, max_vertices = 23) out;

in vec3[1] a;
in vec3[1] b;
in vec3[1] c;
in vec4[1] geoColor;

out vec4 fragColor;

const int NUM_TAIL_SECTIONS = 6;
const int NUM_HEAD_SECTIONS = 4;
const int NUM_EDGE_SECTIONS = NUM_HEAD_SECTIONS + NUM_TAIL_SECTIONS;
const float SECTION_SCALAR = 1.0 / NUM_EDGE_SECTIONS;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform float nodeRadius;
uniform float edgeSize;
uniform float headSize;

vec3 bezier(vec3 A, vec3 B, vec3 C, float u){
    float uinv = 1 - u;
    return A * uinv * uinv + B * 2 * uinv * u + C * u * u;
}
vec3 bezierDerivative(vec3 A, vec3 B, vec3 C, float u){
    float uinv = 1 - u;
    return (B - A) * 2 * uinv + (C - B) * 2 * u;
}

void drawArrowSection(vec3 aPos, vec3 bPos, vec3 cPos, float width, int i){
    float fraction = (i * SECTION_SCALAR);
    vec3 vector = bezier(aPos, bPos, cPos, fraction);
    vec3 direction = bezierDerivative(aPos, bPos, cPos, fraction);

    vec4 scPos = viewMatrix * vec4(vector, 1.0);
    vec4 scDir = viewMatrix * vec4(direction, 0.0);
    vec4 perpendicular = vec4(normalize(vec2(scDir.y, -scDir.x)) * width, 0, 0);

    gl_Position = projectionMatrix * (scPos + perpendicular);
    fragColor = geoColor[0];
    EmitVertex();

    gl_Position = projectionMatrix * (scPos - perpendicular);
    fragColor = geoColor[0];
    EmitVertex();
}

void main() {
    //    mat4 viewProjection = projectionMatrix * viewMatrix;

    float headHSize = 0.5 * headSize;
    float tailHSize = 0.5 * edgeSize;
    // a^2 + b^2 = c^2
    // a = sqrt(c^2 - b^2)
    float adjustedRadius = (headSize > 0) ? nodeRadius : sqrt(nodeRadius * nodeRadius - headHSize * headHSize);

    vec3 aToB = normalize(b[0] - a[0]);
    vec3 bToC = normalize(c[0] - b[0]);
    vec3 aPos = a[0] + aToB * adjustedRadius;
    vec3 bPos = b[0];
    vec3 cPos = c[0] - bToC * nodeRadius;

    vec4 aViewPos = viewMatrix * vec4(aPos, 1.0);
    vec4 bViewPos = viewMatrix * vec4(bPos, 1.0);
    vec4 cViewPos = viewMatrix * vec4(cPos, 1.0);

    vec2 aToBView = vec2(bViewPos.xy - aViewPos.xy);
    vec2 abPerpendicular = normalize(vec2(aToBView.y, -aToBView.x));

    vec2 bToCView = vec2(cViewPos.xy - bViewPos.xy);
    vec2 bcPerpendicular = normalize(vec2(bToCView.y, -bToCView.x));

    float tailFraction = (NUM_EDGE_SECTIONS) / NUM_TAIL_SECTIONS;
    float sectionScalar = 1.0 / (NUM_EDGE_SECTIONS);

    for (int i; i < NUM_TAIL_SECTIONS; i++) {
        drawArrowSection(aPos, bPos, cPos, tailHSize, i);
    }

    drawArrowSection(aPos, bPos, cPos, tailHSize, NUM_TAIL_SECTIONS);

    float growthStep = (headHSize / NUM_HEAD_SECTIONS);
    for (int i = NUM_TAIL_SECTIONS; i < NUM_EDGE_SECTIONS; i++) {
        float width = (NUM_EDGE_SECTIONS - i) * growthStep;
        drawArrowSection(aPos, bPos, cPos, width, i);
    }

    gl_Position = projectionMatrix * cViewPos;
    fragColor = geoColor[0];
    EmitVertex();

    EndPrimitive();
}
