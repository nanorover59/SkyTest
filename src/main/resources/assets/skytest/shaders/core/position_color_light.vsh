#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(std140) uniform LightInfo {
    vec3 LightPos;
    vec3 ViewPos;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 Normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec3 vertexNormal;
layout(location = 2) out vec3 vertexPos;
layout(location = 3) out vec3 lightPos;
layout(location = 4) out vec3 viewPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
    vertexNormal = Normal;
    vertexPos = gl_Position.xyz;
    lightPos = LightPos;
    viewPos = ViewPos;
}