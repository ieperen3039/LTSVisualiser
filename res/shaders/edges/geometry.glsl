#version 330


layout (points) in;
layout (triangle_strip, max_vertices = 23) out;// at most 10 sections

in vec3[1] a;
in vec3[1] b;
in vec3[1] c;
in vec4[1] geoColor;
in int[1] geoID;

out vec4 fragColor;

vec4 color;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform float nodeRadius;
uniform float edgeSize;
uniform float headSize;
uniform bool doUniqueColor;
uniform bool doGradient;
uniform int numTailSections;
uniform int numHeadSections;

vec3 bezier(vec3 A, vec3 B, vec3 C, float u){
    float uinv = 1 - u;
    return A * uinv * uinv + B * 2 * uinv * u + C * u * u;
}
vec3 bezierDerivative(vec3 A, vec3 B, vec3 C, float u){
    float uinv = 1 - u;
    return (B - A) * 2 * uinv + (C - B) * 2 * u;
}

vec4 numberToColor(int i) {
    int bitSize = (1 << 6);
    int r = (i % bitSize) << 2;
    int g = (((i >> 6) % bitSize) << 2);
    int b = (((i >> 12) % bitSize) << 2);

    return vec4(r / 255.0, g / 255.0, b / 255.0, 1.0);
}

void drawArrowSection(vec3 aPos, vec3 bPos, vec3 cPos, float width, float fraction){
    vec3 vector = bezier(aPos, bPos, cPos, fraction);
    vec3 direction = bezierDerivative(aPos, bPos, cPos, fraction);

    vec4 scPos = viewMatrix * vec4(vector, 1.0);
    vec4 scDir = viewMatrix * vec4(direction, 0.0);
    vec4 perpendicular = vec4(normalize(vec2(scDir.y, -scDir.x)) * width, 0, 0);

    gl_Position = projectionMatrix * (scPos + perpendicular);
    fragColor = doGradient ? vec4(color.xyz, (1 - fraction) * color.a) : color;
    EmitVertex();

    gl_Position = projectionMatrix * (scPos - perpendicular);
    fragColor = doGradient ? vec4(color.xyz, (1 - fraction) * color.a) : color;
    EmitVertex();
}

void main() {
    if (doUniqueColor){
        if (geoColor[0].a < 0.09) {
            return;
        }
        else {
            color = numberToColor(geoID);
        }

    } else {
        color = geoColor[0];
    }

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

    int numEdgeSections = numHeadSections + numTailSections;
    float tailFraction = (numEdgeSections) / numTailSections;
    float sectionScalar = 1.0 / numEdgeSections;

    for (int i = 0; i < numTailSections; i++) {
        float fraction = i * sectionScalar;
        drawArrowSection(aPos, bPos, cPos, tailHSize, fraction);
    }

    float fractionOfHeadStart = numTailSections * sectionScalar;
    drawArrowSection(aPos, bPos, cPos, tailHSize, fractionOfHeadStart);

    float growthStep = (headHSize / numHeadSections);
    for (int i = numTailSections; i < numEdgeSections; i++) {
        float fraction = i * sectionScalar;
        float width = (numEdgeSections - i) * growthStep;
        drawArrowSection(aPos, bPos, cPos, width, fraction);
    }

    gl_Position = projectionMatrix * cViewPos;
    fragColor = color;
    EmitVertex();

    EndPrimitive();
}
