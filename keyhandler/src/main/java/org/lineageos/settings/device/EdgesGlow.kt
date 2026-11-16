package org.lineageos.settings.device

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val VERTEX_SHADER_CODE = """
#version 300 es
in vec2 position;
out vec2 uv;
void main() {
    uv = (position + 1.0) * 0.5;
    gl_Position = vec4(position, 0.0, 1.0);
}
"""

private const val FRAGMENT_SHADER_CODE = """
#version 300 es

precision highp float;

in vec2 uv;
out vec4 out_color;

uniform vec2 u_resolution;
uniform float u_time;
uniform vec3 u_glow_color;
uniform float u_strength;
uniform float u_vertical_top;
uniform float u_vertical_bottom;

// ============ ПАРАМЕТРЫ ============
const float CORNER_RADIUS   = 0.05;                    // радиус скругления
const float GLOW_RADIUS     = 0.9;   // ширина ореола
const float GLOW_POWER      = 190.0;    // резкость спада (больше = резче)
const float PULSE_SPEED     = 2.0;    // скорость пульсации
const float PULSE_AMOUNT    = 0.2;    // амплитуда пульсации
// ===================================

// Signed distance to a rounded box
float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    float aspect = u_resolution.x / u_resolution.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
    
    vec2 b = vec2(0.5 * aspect, 0.5);
    float r = min(CORNER_RADIUS, min(b.x, b.y));
    
    float d = sdRoundBox(p, b, r);
    float d_abs = abs(d);
    
    // Свечение
    float glow = 1.0 - smoothstep(0.0, GLOW_RADIUS, d_abs);
    glow = pow(glow, GLOW_POWER);
    
    // Пульсация
    float pulse = 1.0 - PULSE_AMOUNT + PULSE_AMOUNT * sin(u_time * PULSE_SPEED);
    glow *= pulse;
    
    // Неравномерность по вертикали (uv.y = 0 -> верх, uv.y = 1 -> низ)
    float vertical = smoothstep(0.0, 1.0, uv.y);
    glow *= mix(u_vertical_top, u_vertical_bottom, vertical);
    
    // Общая сила свечения
    glow *= u_strength;
    
    float alpha = clamp(glow, 0.0, 1.0);
    vec3 color = u_glow_color * glow;
    out_color = vec4(color, alpha);
}
"""

class EdgesGlowRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var positionHandle = 0
    private var resolutionHandle = 0
    private var timeHandle = 0
    private var glowColorHandle = 0
    private var strengthHandle = 0
    private var verticalTopHandle = 0
    private var verticalBottomHandle = 0

    private var width = 0
    private var height = 0
    private val startTime = System.currentTimeMillis()

    var glowColor: vec3 = vec3(0f, 1f, 0.384f)
    var strength: Float = 1.0f
    var verticalTop: Float = 0.05f
    var verticalBottom: Float = 2.4f

    data class vec3(val r: Float, val g: Float, val b: Float)

    private val vertexData: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).let {
        it.order(ByteOrder.nativeOrder())
        it.asFloatBuffer().apply {
            put(floatArrayOf(
                -1f, -1f,
                 1f, -1f,
                -1f,  1f,
                 1f,  1f
            ))
            position(0)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }

        positionHandle = GLES30.glGetAttribLocation(program, "position")
        resolutionHandle = GLES30.glGetUniformLocation(program, "u_resolution")
        timeHandle = GLES30.glGetUniformLocation(program, "u_time")
        glowColorHandle = GLES30.glGetUniformLocation(program, "u_glow_color")
        strengthHandle = GLES30.glGetUniformLocation(program, "u_strength")
        verticalTopHandle = GLES30.glGetUniformLocation(program, "u_vertical_top")
        verticalBottomHandle = GLES30.glGetUniformLocation(program, "u_vertical_bottom")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        if (program == 0) return

        GLES30.glUseProgram(program)

        // Uniforms
        GLES30.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
        val time = (System.currentTimeMillis() - startTime) / 1000f
        GLES30.glUniform1f(timeHandle, time)
        GLES30.glUniform3f(glowColorHandle, glowColor.r, glowColor.g, glowColor.b)
        GLES30.glUniform1f(strengthHandle, strength)
        GLES30.glUniform1f(verticalTopHandle, verticalTop)
        GLES30.glUniform1f(verticalBottomHandle, verticalBottom)

        // Vertex attribute
        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexData)

        // Draw
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }
}

class EdgesGlow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val renderer: EdgesGlowRenderer
    private var percent = 0f
    private var animator: ValueAnimator? = null
    var onAnimationEnd: (() -> Unit)? = null

    private var colorInt: Int = Color.rgb(0, 255, 98)

    var glowColor: Int
        get() = colorInt
        set(value) {
            colorInt = value
            renderer.glowColor = EdgesGlowRenderer.vec3(
                Color.red(value) / 255f,
                Color.green(value) / 255f,
                Color.blue(value) / 255f
            )
        }

    var isOpen: Boolean = true
        set(value) {
            field = value
            if (!value) {
                renderer.verticalTop = 0.05f
                renderer.verticalBottom = 2.4f
            } else {
                renderer.verticalTop = 2.4f
                renderer.verticalBottom = 0.05f
            }
        }

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        renderer = EdgesGlowRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
    }

    fun startAnimation(isOpen: Boolean, color: Int, onEnd: () -> Unit) {
        this.isOpen = isOpen
        glowColor = color
        onAnimationEnd = onEnd

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            addUpdateListener { anim ->
                percent = anim.animatedValue as Float
                val alpha = when {
                    percent < 0.15f -> percent / 0.15f
                    else -> (1f - percent) / 0.85f
                }.coerceIn(0f, 1f)
                val maxAlpha = if (!this@EdgesGlow.isOpen) 0.6f else 1.0f
                renderer.strength = alpha * maxAlpha
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd?.invoke()
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}
