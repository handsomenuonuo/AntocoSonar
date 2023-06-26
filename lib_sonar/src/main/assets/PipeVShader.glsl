#version 300 es

layout (location = 0)in vec4 aPostion;
layout(location = 1)in vec2 aTexCoord;

uniform mat4 u_Matrix;
out vec2 TexCoord;

void main() {
    gl_Position = u_Matrix * aPostion;
    //纹理坐标传给片段着色器
    TexCoord = aTexCoord;
}
