#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(std140) uniform SphereInfo {
    vec3 SpherePos;
    vec3 LightPos;
    vec3 ViewPos;
    float Radius;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;

layout(location = 0) out vec2 texCoord0;
layout(location = 1) out vec3 spherePos;
layout(location = 2) out vec3 lightPos;
layout(location = 3) out vec3 rayOrigin;
layout(location = 4) out vec3 rayDir;
layout(location = 5) out float radius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    spherePos = SpherePos;
    lightPos = LightPos;
    rayOrigin = ViewPos;
    rayDir = gl_Position.xyz - ViewPos;
}