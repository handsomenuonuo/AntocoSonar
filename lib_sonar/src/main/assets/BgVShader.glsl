#version 300 es

layout(location = 0)in vec4 aPosition;
layout(location = 1)in vec4 aTexCoord;
layout(location = 2)in vec4 bTexCoord;

//uniform mat4 u_Matrix;

out vec2 bgTexCoord;
out vec2 scanTexCoord;

void main() {
    //直接把传入的坐标值作为传入渲染管线。gl_Position是OpenGL内置的
    gl_Position = aPosition;
    //纹理坐标传给片段着色器
    bgTexCoord = aTexCoord.xy;
    scanTexCoord = bTexCoord.xy;
//    scanTexCoord = (u_Matrix * aTexCoord).xy;
}
