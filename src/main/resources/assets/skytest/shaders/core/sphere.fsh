#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec3 spherePos;
layout(location = 2) in vec3 lightPos;
layout(location = 3) in vec3 rayOrigin;
layout(location = 4) in vec3 rayDir;
layout(location = 5) in float radius;

layout(location = 0) out vec4 fragColor;

float sphere(vec3 origin, vec3 dir, vec3 pos, float r) {

}

void main() {
    float rayHit = sphere(rayOrigin, rayDir, spherePos, radius);
    clip(rayHit);

    fragColor = vec4(result, 1.0);
}