#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec3 vertexNormal;
layout(location = 2) in vec3 vertexPos;
layout(location = 3) in vec3 lightPos;
layout(location = 4) in vec3 viewPos;

layout(location = 0) out vec4 fragColor;

void main() {
    if (vertexColor.a == 0.0) {
        discard;
    }

    vec3 materialColor = vertexColor.rgb;
    vec3 lightColor = vec3(1.0, 1.0, 1.0);

    vec3 norm = normalize(vertexNormal);
    vec3 lightDir = normalize(lightPos - vertexPos);
    vec3 reflectDir = reflect(lightDir, norm);
    vec3 viewDir = normalize(viewPos - vertexPos);

    float diff = max(dot(norm, lightDir), 0.2);
    vec3 diffuse = diff * lightColor;

    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 128);
    vec3 specular = spec * lightColor * 0.25;

    vec3 result = (diffuse + specular) * materialColor;
    fragColor = vec4(result, 1.0);
}