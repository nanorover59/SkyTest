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

/*
* Sphere Intersect
* Returns the length of the ray from the from the origin to the sphere surface
* or -1.0 if the sphere is missed
*/
float sphere(vec3 origin, vec3 dir, vec4 sphere) {
    vec3 oc = rayOrigin - sphere.xyz;
    float b = dot(oc, dir);
    float c = dot(oc, oc) - sphere.w * sphere.w;
    float h = b * b - c;
    if(h < 0.0)
        return -1.0;
    else
        return -b - sqrt(h);
}

void main() {
    float rayHit = sphere(rayOrigin, rayDir, spherePos, radius);
    clip(rayHit);

    vec3 pos = rayDir * rayHit + rayOrigin; // Position on the sphere
    vec3 norm = normalize(pos - spherePos); // Surface normal unit vector
    vec3 lightDir = normalize(lightPos);

    float diff = max(dot(norm, lightDir), 0.2);
    vec3 diffuse = diff * lightColor;

    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 64);
    vec3 specular = spec * lightColor * 0.25;

    vec3 result = (diffuse + specular) * materialColor;
    fragColor = vec4(result, 1.0);
}