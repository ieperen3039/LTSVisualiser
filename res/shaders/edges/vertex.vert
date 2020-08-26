#version 330

layout (location = 0) in vec3 a_in;
layout (location = 1) in vec3 b_in;
layout (location = 2) in vec3 c_in;
layout (location = 3) in vec4 color_in;

out vec3 a;// start
out vec3 b;// middle
out vec3 c;// end
out vec4 geoColor;

void main(){
    a = a_in;
    b = b_in;
    c = c_in;
    geoColor = color_in;
}
