#version 300 es

layout (location = 0)in vec4 aPostion;

uniform mat4 u_Matrix;

void main() {
    gl_Position = u_Matrix * aPostion;
}
