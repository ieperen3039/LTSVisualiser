#version 330

const float THICKNESS = 0.15f;

uniform bool doUniqueColor;

smooth in float distanceFromMiddle;
in vec4 fragColor;
in vec4 fragBorder;

out vec4 outputColor;

void main()
{
    if (doUniqueColor || distanceFromMiddle < (1 - THICKNESS)){
        outputColor = fragColor;

    } else {
        outputColor = fragBorder;
    }
}
