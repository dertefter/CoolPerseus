package com.dertefter.shared.slider_animations

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "EdgesGlow"

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
uniform float u_corner_radius;
uniform float u_glow_radius;
uniform float u_glow_power;
uniform float u_pulse_speed;
uniform float u_pulse_amount;

// Signed distance to a rounded box
float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    float aspect = u_resolution.x / u_resolution.y;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
    
    vec2 b = vec2(0.5 * aspect, 0.5);
    float r = min(u_corner_radius, min(b.x, b.y));
    
    float d = sdRoundBox(p, b, r);
    float d_abs = abs(d);
    
    // Свечение
    float glow_rad = max(u_glow_radius, 0.001);
    float glow = 1.0 - smoothstep(0.0, glow_rad, d_abs);
    glow = pow(max(glow, 0.00001), max(u_glow_power, 0.001));
    
    // Пульсация
    float pulse = 1.0 - u_pulse_amount + u_pulse_amount * sin(u_time * u_pulse_speed);
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
    private var cornerRadiusHandle = 0
    private var glowRadiusHandle = 0
    private var glowPowerHandle = 0
    private var pulseSpeedHandle = 0
    private var pulseAmountHandle = 0

    private var width = 0
    private var height = 0
    private var firstFrameLogged = false
    private val startTime = System.currentTimeMillis()

    var onSurfaceReady: (() -> Unit)? = null

    var glowColor: Vec3 = Vec3(0f, 1f, 0.384f)
    var strength: Float = 1.0f
    var verticalTop: Float = 0.05f
    var verticalBottom: Float = 0.4f

    var cornerRadius: Float = EdgesGlowSettings.DEFAULT_CORNER_RADIUS
    var glowRadius: Float = EdgesGlowSettings.DEFAULT_GLOW_RADIUS
    var glowPower: Float = EdgesGlowSettings.DEFAULT_GLOW_POWER
    var pulseSpeed: Float = EdgesGlowSettings.DEFAULT_PULSE_SPEED
    var pulseAmount: Float = EdgesGlowSettings.DEFAULT_PULSE_AMOUNT

    data class Vec3(val r: Float, val g: Float, val b: Float)

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

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link failed: ${GLES30.glGetProgramInfoLog(program)}")
            GLES30.glDeleteProgram(program)
            program = 0
            return
        }

        positionHandle = GLES30.glGetAttribLocation(program, "position")
        resolutionHandle = GLES30.glGetUniformLocation(program, "u_resolution")
        timeHandle = GLES30.glGetUniformLocation(program, "u_time")
        glowColorHandle = GLES30.glGetUniformLocation(program, "u_glow_color")
        strengthHandle = GLES30.glGetUniformLocation(program, "u_strength")
        verticalTopHandle = GLES30.glGetUniformLocation(program, "u_vertical_top")
        verticalBottomHandle = GLES30.glGetUniformLocation(program, "u_vertical_bottom")
        cornerRadiusHandle = GLES30.glGetUniformLocation(program, "u_corner_radius")
        glowRadiusHandle = GLES30.glGetUniformLocation(program, "u_glow_radius")
        glowPowerHandle = GLES30.glGetUniformLocation(program, "u_glow_power")
        pulseSpeedHandle = GLES30.glGetUniformLocation(program, "u_pulse_speed")
        pulseAmountHandle = GLES30.glGetUniformLocation(program, "u_pulse_amount")

        val invalid = listOf(
            "position" to positionHandle,
            "u_resolution" to resolutionHandle,
            "u_glow_color" to glowColorHandle,
            "u_strength" to strengthHandle,
            "u_vertical_top" to verticalTopHandle,
            "u_vertical_bottom" to verticalBottomHandle,
            "u_corner_radius" to cornerRadiusHandle,
            "u_glow_radius" to glowRadiusHandle,
            "u_glow_power" to glowPowerHandle,
            "u_pulse_speed" to pulseSpeedHandle,
            "u_pulse_amount" to pulseAmountHandle
        ).filter { it.second < 0 }.joinToString { it.first }

        Log.d(TAG, "onSurfaceCreated: program=$program invalidUniforms=[$invalid]")

        onSurfaceReady?.invoke()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        Log.d(TAG, "onSurfaceChanged: ${width}x$height")
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
        GLES30.glUniform1f(cornerRadiusHandle, cornerRadius)
        GLES30.glUniform1f(glowRadiusHandle, glowRadius)
        GLES30.glUniform1f(glowPowerHandle, glowPower)
        GLES30.glUniform1f(pulseSpeedHandle, pulseSpeed)
        GLES30.glUniform1f(pulseAmountHandle, pulseAmount)

        if (!firstFrameLogged) {
            firstFrameLogged = true
            Log.d(
                TAG,
                "firstFrame: size=${width}x$height strength=$strength color=$glowColor " +
                        "vTop=$verticalTop vBottom=$verticalBottom corner=$cornerRadius " +
                        "radius=$glowRadius power=$glowPower speed=$pulseSpeed amount=$pulseAmount"
            )
        }

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
            val status = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                Log.e(TAG, "Shader compile failed (type=$type): ${GLES30.glGetShaderInfoLog(shader)}")
            }
        }
    }
}

class EdgesGlow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    zOrderOnTop: Boolean = false
) : GLSurfaceView(context, attrs), SliderAnimation {
    private val renderer = EdgesGlowRenderer()

    private var percent = 0f
    private var animator: ValueAnimator? = null
    override var onAnimationEnd: (() -> Unit)? = null

    private var colorInt: Int = Color.rgb(0, 255, 98)

    private var surfaceReady = false
    private var pendingStart: (() -> Unit)? = null

    private val surfaceTimeout = Runnable {
        if (!surfaceReady) {
            Log.w(TAG, "GL surface is not ready after $SURFACE_TIMEOUT_MS ms, starting anyway")
            surfaceReady = true
            val pending = pendingStart
            pendingStart = null
            pending?.invoke()
        }
    }

    var maxVerticalTop: Float = EdgesGlowSettings.DEFAULT_MAX_VERTICAL_TOP
        set(value) {
            field = value
            updateVerticals()
        }

    var maxVerticalBottom: Float = EdgesGlowSettings.DEFAULT_MAX_VERTICAL_BOTTOM
        set(value) {
            field = value
            updateVerticals()
        }

    var cornerRadius: Float
        get() = renderer.cornerRadius
        set(value) { renderer.cornerRadius = value }

    var glowRadius: Float
        get() = renderer.glowRadius
        set(value) { renderer.glowRadius = value }

    var glowPower: Float
        get() = renderer.glowPower
        set(value) { renderer.glowPower = value }

    var pulseSpeed: Float
        get() = renderer.pulseSpeed
        set(value) { renderer.pulseSpeed = value }

    var pulseAmount: Float
        get() = renderer.pulseAmount
        set(value) { renderer.pulseAmount = value }

    var glowColor: Int
        get() = colorInt
        set(value) {
            colorInt = value
            renderer.glowColor = EdgesGlowRenderer.Vec3(
                Color.red(value) / 255f,
                Color.green(value) / 255f,
                Color.blue(value) / 255f
            )
        }

    override var isOpen: Boolean = true
        set(value) {
            field = value
            updateVerticals()
        }

    private fun updateVerticals() {
        if (!isOpen) {
            renderer.verticalTop = 0.05f
            renderer.verticalBottom = maxVerticalBottom
        } else {
            renderer.verticalTop = maxVerticalTop
            renderer.verticalBottom = 0.05f
        }
    }

    fun applySettings(settings: EdgesGlowSettings) {
        val defaults = EdgesGlowSettings()
        cornerRadius = settings.cornerRadius.safe(defaults.cornerRadius, 0f..0.5f)
        glowRadius = settings.glowRadius.safe(defaults.glowRadius, 0.01f..5f)
        glowPower = settings.glowPower.safe(defaults.glowPower, 0.01f..1000f)
        pulseSpeed = settings.pulseSpeed.safe(defaults.pulseSpeed, 0f..100f)
        pulseAmount = settings.pulseAmount.safe(defaults.pulseAmount, 0f..1f)
        maxVerticalTop = settings.maxVerticalTop.safe(defaults.maxVerticalTop, 0f..10f)
        maxVerticalBottom = settings.maxVerticalBottom.safe(defaults.maxVerticalBottom, 0f..10f)
    }

    private fun Float.safe(fallback: Float, range: ClosedFloatingPointRange<Float>): Float =
        if (isFinite() && this in range) this else fallback

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        holder.setFormat(PixelFormat.TRANSLUCENT)
        if (zOrderOnTop) {
            setZOrderOnTop(true)
        }
        isClickable = false
        isFocusable = false

        renderer.onSurfaceReady = {
            post {
                removeCallbacks(surfaceTimeout)
                if (!surfaceReady) {
                    surfaceReady = true
                    val pending = pendingStart
                    pendingStart = null
                    pending?.invoke()
                }
            }
        }
    }

    override fun startAnimation(isOpen: Boolean, color: Int, onEnd: () -> Unit) {
        this.isOpen = isOpen
        glowColor = color
        onAnimationEnd = onEnd

        if (!surfaceReady) {
            pendingStart = { runAnimator() }
            removeCallbacks(surfaceTimeout)
            postDelayed(surfaceTimeout, SURFACE_TIMEOUT_MS)
            Log.d(TAG, "startAnimation deferred until GL surface is ready")
            return
        }

        runAnimator()
    }

    private fun runAnimator() {
        Log.d(TAG, "startAnimation: isOpen=$isOpen color=${Integer.toHexString(colorInt)}")

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
        removeCallbacks(surfaceTimeout)
        pendingStart = null
        animator?.cancel()
    }

    companion object {
        private const val SURFACE_TIMEOUT_MS = 400L
    }
}
