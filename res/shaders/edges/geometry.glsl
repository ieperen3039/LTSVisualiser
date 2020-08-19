#version 330

layout (points) in;
layout (triangle_strip, max_vertices = 7) out;

in vec3[1] a;
in vec3[1] b;
in vec4[1] geoColor;

out vec4 fragColor;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform float nodeRadius;
uniform float arrowSize;
uniform float headSize;

void main() {
    mat4 viewProjection = projectionMatrix * viewMatrix;

    float arrowHSize = 0.5 * arrowSize;
    float bodyHSize = headSize / 10;
    // a^2 + b^2 = c^2
    // a = sqrt(c^2 - b^2)
    float adjustedRadius = sqrt(nodeRadius * nodeRadius - arrowHSize * arrowHSize);

    vec3 aToB = normalize(b[0] - a[0]);
    vec3 aPos = a[0] + aToB * adjustedRadius;
    vec3 bPos = b[0] - aToB * nodeRadius;

    vec4 aScPos = viewProjection * vec4(aPos, 1.0) + vec4(0.0, 0.0, 1.0/32, 0.0); // prevents z-fighting;
    vec4 bScPos = viewProjection * vec4(bPos, 1.0);
    vec2 aScToBSc = vec2(bScPos.xy - aScPos.xy);
    vec2 aPerpendicular = normalize(vec2(aScToBSc.y, -aScToBSc.x));

    vec4 headEnd;
    if (headSize > 0){
        headEnd = viewProjection * vec4(bPos - aToB * headSize, 1.0);
        vec4 bodyOffset = projectionMatrix * vec4(aPerpendicular * bodyHSize, 0.0, 0.0);

        gl_Position = aScPos + bodyOffset;
        fragColor = geoColor[0];
        EmitVertex();

        gl_Position = aScPos - bodyOffset;
        fragColor = geoColor[0];
        EmitVertex();

        gl_Position = headEnd + bodyOffset;
        fragColor = geoColor[0];
        EmitVertex();

        gl_Position = headEnd - bodyOffset;
        fragColor = geoColor[0];
        EmitVertex();

    } else {
        headEnd = aScPos;
    }

    vec4 headOffset = projectionMatrix * vec4(aPerpendicular * arrowHSize, 0.0, 0.0);
    gl_Position = headEnd + headOffset;
    fragColor = geoColor[0];
    EmitVertex();

    gl_Position = headEnd - headOffset;
    fragColor = geoColor[0];
    EmitVertex();

    gl_Position = bScPos;
    fragColor = geoColor[0];
    EmitVertex();

    EndPrimitive();
}
