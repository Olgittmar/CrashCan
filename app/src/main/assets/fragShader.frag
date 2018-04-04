#version 320 es

#ifdef GL_ES
precision mediump float;
#endif

in vec4 fColor;
out vec4 fragmentColor;

void main() {
    fragmentColor = fColor;
}