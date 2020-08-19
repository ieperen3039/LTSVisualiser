#version 330

layout (location = 0) in vec3 center;// position of the middle of the triangle at t = 0
layout (location = 1) in vec4 color;// position of the middle of the triangle at t = 0


uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

out vec4 geoMiddle;// position of middle
out float nodeSizeScalar;
out vec4 geoColor;

void main(){
    // exciting
    geoMiddle = projectionMatrix * viewMatrix * vec4(center, 1.0);
    geoColor = color;
}
