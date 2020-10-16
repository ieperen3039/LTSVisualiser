#version 330

layout (location = 0) in vec3 center;// position of the middle of the triangle at t = 0
layout (location = 1) in vec4 color;// inner color
layout (location = 2) in vec4 border;// outer border color

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

out vec4 geoMiddle;
out vec4 geoColor;
out vec4 geoBorder;
out int geoID;

void main(){
    geoMiddle = projectionMatrix * viewMatrix * vec4(center, 1.0);
    geoColor = color;
    geoID = gl_VertexID + 1;
    geoBorder = border;
}
