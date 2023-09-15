#version 300 es

precision mediump float;

uniform vec4 uTextColor;//输出的颜色

out vec4 FragColor;

void main() {
//    FragColor = vec4(1.0f,0.5f,0.2f,1.0f);
    FragColor = uTextColor;
}
