#version 300 es

precision mediump float;
in vec2 bgTexCoord;
in vec2 scanTexCoord;
out vec4 FragColor;
uniform sampler2D bgTexture;
uniform sampler2D scanTexture;

void main() {
//    FragColor = texture(bgTexture, bgTexCoord);
//    FragColor = mix(texture(bgTexture, bgTexCoord), texture(scanTexture, scanTexCoord), 0.5);
    FragColor = texture(bgTexture, bgTexCoord) + texture(scanTexture, scanTexCoord);
}
