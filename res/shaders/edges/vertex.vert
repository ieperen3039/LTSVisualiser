#version 330

layout (location = 0) in vec3 a_in;
layout (location = 1) in vec3 b_in;
layout (location = 2) in vec4 color_in;

out vec3 a;// position of middle
out vec3 b;// position of middle
out vec4 geoColor;// position of middle

void main(){
    a = a_in;
    b = b_in;
    geoColor = color_in;
}
