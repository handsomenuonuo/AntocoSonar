#version 300 es

layout (location = 0)
in vec4 aPostion;

//layout (location = 1)
//in vec4 aColor;

//out vec4 uTextColor;

void main() {
    gl_Position =  aPostion;
//    uTextColor = aColor;
}
