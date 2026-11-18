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

private const val TAG = "PixelGlow"

private const val PIXEL_VERTEX_SHADER_CODE = """
#version 300 es
in vec2 position;
out vec2 uv;
void main() {
    uv = (position + 1.0) * 0.5;
    gl_Position = vec4(position, 0.0, 1.0);
}
"""

private const val PIXEL_FRAGMENT_SHADER_CODE = """
#version 300 es

precision highp float;

in vec2 uv;
out vec4 out_color;

uniform vec2 u_resolution;
uniform float u_time;
uniform vec3 u_glow_color;
uniform float u_strength;
uniform float u_glow_strength;
uniform float u_corner_radius;
uniform float u_glow_radius;
uniform float u_glow_power;
uniform float u_pulse_speed;
uniform float u_pulse_amount;
uniform float u_vertical_top;
uniform float u_vertical_bottom;
uniform float u_dot_cell;
uniform float u_dot_size;
uniform float u_dot_density;
uniform float u_dot_flicker;

// Signed distance to a rounded box
float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// Хэш 2D -> 2D (для случайного смещения точки внутри ячейки)
vec2 hash22(vec2 p) {
    p = vec2(dot(p, vec2(127.1, 311.7)),
             dot(p, vec2(269.5, 183.3)));
    return fract(sin(p) * 43758.5453);
}

// Хэш 2D -> float
float hash21(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    float height = max(u_resolution.y, 1.0);
    float aspect = u_resolution.x / height;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);

    vec2 b = vec2(0.5 * aspect, 0.5);
    float r = min(u_corner_radius, min(b.x, b.y));

    float d = sdRoundBox(p, b, r);
    float d_abs = abs(d);

    // --- Базовое свечение (форма рамки) ---
    // max(...) нужен, чтобы pow(0.0, ...) не давал NaN на мобильных GPU.
    float glow_rad = max(u_glow_radius, 0.001);
    float glow = 1.0 - smoothstep(0.0, glow_rad, d_abs);
    glow = pow(max(glow, 0.00001), max(u_glow_power, 0.001));

    float pulse = 1.0 - u_pulse_amount + u_pulse_amount * sin(u_time * u_pulse_speed);
    glow *= pulse;

    float vertical = smoothstep(0.0, 1.0, uv.y);
    glow *= mix(u_vertical_top, u_vertical_bottom, vertical);
    glow *= u_glow_strength * u_strength;

    // --- Пиксельный шум ---
    float cell = max(u_dot_cell, 0.0001);
    vec2 grid_uv = uv * u_resolution / (cell * height);
    vec2 cell_id = floor(grid_uv);
    vec2 cell_uv = fract(grid_uv) - 0.5;

    float dot_size = clamp(u_dot_size, 0.0, 0.5);
    vec2 jitter = (hash22(cell_id) - 0.5) * (1.0 - 2.0 * dot_size);
    float dist_to_dot = length(cell_uv - jitter);

    float dot_mask = 1.0 - smoothstep(max(dot_size - 0.05, 0.0), dot_size + 0.05, dist_to_dot);

    float cell_rand = hash21(cell_id);
    float is_on = step(cell_rand, u_dot_density);

    float phase = hash21(cell_id + 17.0) * 6.2831853;
    float flicker = 0.5 + 0.5 * sin(u_time * u_dot_flicker + phase);

    // --- Итог: шум проявляет свечение ---
    float intensity = clamp(glow * dot_mask * is_on * flicker, 0.0, 1.0);

    // Альфа обязательно премноженная: при out_color = vec4(color, 1.0) оверлейное
    // окно с GL_BLEND(G_ONE, GL_ONE_MINUS_SRC_ALPHA) закрыло бы весь экран чёрным.
    vec3 color = u_glow_color * intensity;
    out_color = vec4(color, intensity);
}
"""

class PixelGlowRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var positionHandle = 0
    private var resolutionHandle = 0
    private var timeHandle = 0
    private var glowColorHandle = 0
    private var strengthHandle = 0
    private var glowStrengthHandle = 0
    private var cornerRadiusHandle = 0
    private var glowRadiusHandle = 0
    private var glowPowerHandle = 0
    private var pulseSpeedHandle = 0
    private var pulseAmountHandle = 0
    private var verticalTopHandle = 0
    private var verticalBottomHandle = 0
    private var dotCellHandle = 0
    private var dotSizeHandle = 0
    private var dotDensityHandle = 0
    private var dotFlickerHandle = 0

    private var width = 0
    private var height = 0
    private var firstFrameLogged = false
    private val startTime = System.currentTimeMillis()

    /** Вызывается на GL-потоке, когда поверхность и программа готовы к отрисовке. */
    var onSurfaceReady: (() -> Unit)? = null

    var glowColor: EdgesGlowRenderer.Vec3 = EdgesGlowRenderer.Vec3(0f, 1f, 0.384f)
    var strength: Float = 1.0f

    var glowStrength: Float = PixelGlowSettings.DEFAULT_GLOW_STRENGTH
    var cornerRadius: Float = PixelGlowSettings.DEFAULT_CORNER_RADIUS
    var glowRadius: Float = PixelGlowSettings.DEFAULT_GLOW_RADIUS
    var glowPower: Float = PixelGlowSettings.DEFAULT_GLOW_POWER
    var pulseSpeed: Float = PixelGlowSettings.DEFAULT_PULSE_SPEED
    var pulseAmount: Float = PixelGlowSettings.DEFAULT_PULSE_AMOUNT
    var verticalTop: Float = PixelGlowSettings.DEFAULT_VERTICAL_TOP
    var verticalBottom: Float = PixelGlowSettings.DEFAULT_VERTICAL_BOTTOM
    var dotCell: Float = PixelGlowSettings.DEFAULT_DOT_CELL
    var dotSize: Float = PixelGlowSettings.DEFAULT_DOT_SIZE
    var dotDensity: Float = PixelGlowSettings.DEFAULT_DOT_DENSITY
    var dotFlicker: Float = PixelGlowSettings.DEFAULT_DOT_FLICKER

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

        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, PIXEL_VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, PIXEL_FRAGMENT_SHADER_CODE)

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
        glowStrengthHandle = GLES30.glGetUniformLocation(program, "u_glow_strength")
        cornerRadiusHandle = GLES30.glGetUniformLocation(program, "u_corner_radius")
        glowRadiusHandle = GLES30.glGetUniformLocation(program, "u_glow_radius")
        glowPowerHandle = GLES30.glGetUniformLocation(program, "u_glow_power")
        pulseSpeedHandle = GLES30.glGetUniformLocation(program, "u_pulse_speed")
        pulseAmountHandle = GLES30.glGetUniformLocation(program, "u_pulse_amount")
        verticalTopHandle = GLES30.glGetUniformLocation(program, "u_vertical_top")
        verticalBottomHandle = GLES30.glGetUniformLocation(program, "u_vertical_bottom")
        dotCellHandle = GLES30.glGetUniformLocation(program, "u_dot_cell")
        dotSizeHandle = GLES30.glGetUniformLocation(program, "u_dot_size")
        dotDensityHandle = GLES30.glGetUniformLocation(program, "u_dot_density")
        dotFlickerHandle = GLES30.glGetUniformLocation(program, "u_dot_flicker")

        val invalid = listOf(
            resolutionHandle, glowColorHandle, strengthHandle, glowStrengthHandle,
            cornerRadiusHandle, glowRadiusHandle, glowPowerHandle, pulseSpeedHandle,
            pulseAmountHandle, verticalTopHandle, verticalBottomHandle, dotCellHandle,
            dotSizeHandle, dotDensityHandle, dotFlickerHandle
        ).count { it < 0 }

        Log.d(TAG, "onSurfaceCreated: program=$program invalidUniforms=$invalid")

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

        GLES30.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
        val time = (System.currentTimeMillis() - startTime) / 1000f
        GLES30.glUniform1f(timeHandle, time)
        GLES30.glUniform3f(glowColorHandle, glowColor.r, glowColor.g, glowColor.b)
        GLES30.glUniform1f(strengthHandle, strength)
        GLES30.glUniform1f(glowStrengthHandle, glowStrength)
        GLES30.glUniform1f(cornerRadiusHandle, cornerRadius)
        GLES30.glUniform1f(glowRadiusHandle, glowRadius)
        GLES30.glUniform1f(glowPowerHandle, glowPower)
        GLES30.glUniform1f(pulseSpeedHandle, pulseSpeed)
        GLES30.glUniform1f(pulseAmountHandle, pulseAmount)
        GLES30.glUniform1f(verticalTopHandle, verticalTop)
        GLES30.glUniform1f(verticalBottomHandle, verticalBottom)
        GLES30.glUniform1f(dotCellHandle, dotCell)
        GLES30.glUniform1f(dotSizeHandle, dotSize)
        GLES30.glUniform1f(dotDensityHandle, dotDensity)
        GLES30.glUniform1f(dotFlickerHandle, dotFlicker)

        if (!firstFrameLogged) {
            firstFrameLogged = true
            Log.d(
                TAG,
                "firstFrame: size=${width}x$height strength=$strength color=$glowColor " +
                        "radius=$glowRadius power=$glowPower cell=$dotCell size=$dotSize " +
                        "density=$dotDensity flicker=$dotFlicker"
            )
        }

        GLES30.glEnableVertexAttribArray(positionHandle)
        GLES30.glVertexAttribPointer(positionHandle, 2, GLES30.GL_FLOAT, false, 0, vertexData)

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

class PixelGlow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    zOrderOnTop: Boolean = false
) : GLSurfaceView(context, attrs), SliderAnimation {

    private val renderer = PixelGlowRenderer()

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

    var glowStrength: Float
        get() = renderer.glowStrength
        set(value) { renderer.glowStrength = value }

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

    var verticalTop: Float = PixelGlowSettings.DEFAULT_VERTICAL_TOP
        set(value) {
            field = value
            updateVerticals()
        }

    var verticalBottom: Float = PixelGlowSettings.DEFAULT_VERTICAL_BOTTOM
        set(value) {
            field = value
            updateVerticals()
        }

    var dotCell: Float
        get() = renderer.dotCell
        set(value) { renderer.dotCell = value }

    var dotSize: Float
        get() = renderer.dotSize
        set(value) { renderer.dotSize = value }

    var dotDensity: Float
        get() = renderer.dotDensity
        set(value) { renderer.dotDensity = value }

    var dotFlicker: Float
        get() = renderer.dotFlicker
        set(value) { renderer.dotFlicker = value }

    override var isOpen: Boolean = true
        set(value) {
            field = value
            updateVerticals()
        }

    private fun updateVerticals() {
        if (isOpen) {
            renderer.verticalTop = verticalTop
            renderer.verticalBottom = verticalBottom
        } else {
            renderer.verticalTop = verticalBottom
            renderer.verticalBottom = verticalTop
        }
    }

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

    fun applySettings(settings: PixelGlowSettings) {
        val defaults = PixelGlowSettings()
        cornerRadius = settings.cornerRadius.safe(defaults.cornerRadius, 0f..0.5f)
        glowRadius = settings.glowRadius.safe(defaults.glowRadius, 0.01f..5f)
        glowPower = settings.glowPower.safe(defaults.glowPower, 0.01f..1000f)
        pulseSpeed = settings.pulseSpeed.safe(defaults.pulseSpeed, 0f..100f)
        pulseAmount = settings.pulseAmount.safe(defaults.pulseAmount, 0f..1f)
        verticalTop = settings.verticalTop.safe(defaults.verticalTop, 0f..10f)
        verticalBottom = settings.verticalBottom.safe(defaults.verticalBottom, 0f..10f)
        glowStrength = settings.glowStrength.safe(defaults.glowStrength, 0f..5f)
        dotCell = settings.dotCell.safe(defaults.dotCell, 0.0002f..0.1f)
        dotSize = settings.dotSize.safe(defaults.dotSize, 0f..0.5f)
        dotDensity = settings.dotDensity.safe(defaults.dotDensity, 0f..1f)
        dotFlicker = settings.dotFlicker.safe(defaults.dotFlicker, 0f..50f)
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
            duration = ANIMATION_DURATION_MS
            addUpdateListener { anim ->
                percent = anim.animatedValue as Float
                val alpha = when {
                    percent < 0.15f -> percent / 0.15f
                    else -> (1f - percent) / 0.85f
                }.coerceIn(0f, 1f)
                val maxAlpha = if (!this@PixelGlow.isOpen) CLOSE_ALPHA else OPEN_ALPHA
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
        private const val ANIMATION_DURATION_MS = 600L
        private const val SURFACE_TIMEOUT_MS = 400L
        private const val OPEN_ALPHA = 1.0f
        private const val CLOSE_ALPHA = 0.6f
    }
}
