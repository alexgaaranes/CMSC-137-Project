#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 dir;
uniform vec2 resolution;

void main() {
    vec4 sum = vec4(0.0);
    vec2 tc = v_texCoords;
    vec2 blur = dir / resolution; 

    // 9-tap gaussian blur
    sum += texture2D(u_texture, vec2(tc.x - 4.0*blur.x, tc.y - 4.0*blur.y)) * 0.05;
    sum += texture2D(u_texture, vec2(tc.x - 3.0*blur.x, tc.y - 3.0*blur.y)) * 0.09;
    sum += texture2D(u_texture, vec2(tc.x - 2.0*blur.x, tc.y - 2.0*blur.y)) * 0.12;
    sum += texture2D(u_texture, vec2(tc.x - 1.0*blur.x, tc.y - 1.0*blur.y)) * 0.15;
    
    sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.18;
    
    sum += texture2D(u_texture, vec2(tc.x + 1.0*blur.x, tc.y + 1.0*blur.y)) * 0.15;
    sum += texture2D(u_texture, vec2(tc.x + 2.0*blur.x, tc.y + 2.0*blur.y)) * 0.12;
    sum += texture2D(u_texture, vec2(tc.x + 3.0*blur.x, tc.y + 3.0*blur.y)) * 0.09;
    sum += texture2D(u_texture, vec2(tc.x + 4.0*blur.x, tc.y + 4.0*blur.y)) * 0.05;

    gl_FragColor = v_color * sum;
}
