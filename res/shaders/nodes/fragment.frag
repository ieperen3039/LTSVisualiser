#version 330

const float THICKNESS = 0.10f;

smooth in float distanceFromMiddle;
in vec4 fragColor;

out vec4 outputColor;

void main()
{
    if (distanceFromMiddle > (1 - THICKNESS)){
        // black
        outputColor = vec4(0.0, 0.0, 0.0, 1.0);
    } else {
        outputColor = fragColor;
    }
}
