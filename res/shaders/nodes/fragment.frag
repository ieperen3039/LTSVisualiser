#version 330

const float THICKNESS = 0.10f;

uniform bool doUniqueColor;

smooth in float distanceFromMiddle;
in vec4 fragColor;

out vec4 outputColor;

void main()
{
    if (doUniqueColor || distanceFromMiddle < (1 - THICKNESS)){
        outputColor = fragColor;

    } else {
        // black
        outputColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
