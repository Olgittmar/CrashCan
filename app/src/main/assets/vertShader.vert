#version 320 es
in vec3 vPosition;
in vec3 vNormal;
uniform mat4 uMVPMatrix;
uniform vec4 vColor;
out vec4 fColor;

void main() {

    vec4 homPos = vec4(vPosition, 1.0f);
    gl_Position = uMVPMatrix * homPos;
    vec3 tColor = vec3(vColor);
    float intensity = abs(dot(tColor, normalize(vNormal)));
    fColor = vColor*intensity;
}