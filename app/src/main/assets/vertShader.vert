#version 320 es
in vec3 vPosition;
in vec3 vNormal;
uniform mat4 uMVPMatrix;
uniform vec4 vColor;
uniform vec3 vLight;
out vec4 fColor;

void main() {

    vec4 homPos = vec4(vPosition, 1.0f);
    gl_Position = uMVPMatrix * homPos;
    vec3 lightDir = vPosition - vLight;
    float intensity = clamp(abs(dot(lightDir,vNormal)),0.2f, 1.0f);
    fColor = vColor*intensity;
}