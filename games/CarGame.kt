package com.example.cargame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(CarGameView(this))
    }
}

class CarGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Runnable {

    private val paintRoad = Paint().apply { color = Color.DKGRAY }
    private val paintLine = Paint().apply {
        color = Color.WHITE
        strokeWidth = 10f
        pathEffect = DashPathEffect(floatArrayOf(40f, 40f), 0f)
    }
    private val paintCar = Paint().apply { color = Color.CYAN }
    private val paintEnemy = Paint().apply { color = Color.RED }
    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 64f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    private val paintGameOver = Paint().apply {
        color = Color.YELLOW
        textSize = 96f
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }

    private var carWidth = 0f
    private var carHeight = 0f
    private var carX = 0f
    private var carY = 0f

    data class EnemyCar(var x: Float, var y: Float, var speed: Float)
    private val enemies = mutableListOf<EnemyCar>()

    private var gameRunning = true
    private var score = 0
    private var speedBase = 12f
    private var lastSpawnTime = 0L
    private var spawnInterval = 800L

    private var lastMoveTime = 0L
    private val moveCooldown = 100L

    init {
        post(this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        carWidth = w / 6f
        carHeight = h / 6f
        carX = width / 2f - carWidth / 2f
        carY = height - carHeight - 60f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintRoad)

        val path = Path()
        path.moveTo(width / 2f, 0f)
        path.lineTo(width / 2f, height.toFloat())
        canvas.drawPath(path, paintLine)

        val carRect = RectF(carX, carY, carX + carWidth, carY + carHeight)
        canvas.drawRoundRect(carRect, 30f, 30f, paintCar)

        enemies.forEach {
            val r = RectF(it.x, it.y, it.x + carWidth, it.y + carHeight)
            canvas.drawRoundRect(r, 30f, 30f, paintEnemy)
        }

        canvas.drawText("Score : $score", 40f, 80f, paintText)

        if (!gameRunning) {
            val text = "GAME OVER"
            val textWidth = paintGameOver.measureText(text)
            canvas.drawText(text, width / 2f - textWidth / 2f, height / 2f, paintGameOver)

            val retry = "Touchez pour rejouer"
            val retryWidth = paintText.measureText(retry)
            canvas.drawText(retry, width / 2f - retryWidth / 2f, height / 2f + 120f, paintText)
        }
    }

    override fun run() {
        if (gameRunning) {
            updateGame()
        }
        invalidate()
        postDelayed(this, 16L)
    }

    private fun updateGame() {
        val now = System.currentTimeMillis()

        enemies.forEach {
            it.y += it.speed
        }

        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (e.y > height) {
                iterator.remove()
                score += 1
                speedBase = min(speedBase + 0.2f, 40f)
                spawnInterval = max(350L, spawnInterval - 10L)
            }
        }

        if (now - lastSpawnTime > spawnInterval) {
            lastSpawnTime = now
            val lane = Random.nextInt(0, 3)
            val laneWidth = width / 3f
            val x = lane * laneWidth + laneWidth / 2f - carWidth / 2f
            enemies.add(EnemyCar(x, -carHeight, speedBase + Random.nextFloat() * 5f))
        }

        val carRect = RectF(carX, carY, carX + carWidth, carY + carHeight)
        enemies.forEach {
            val r = RectF(it.x, it.y, it.x + carWidth, it.y + carHeight)
            if (RectF.intersects(carRect, r)) {
                gameRunning = false
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            if (!gameRunning) {
                resetGame()
                return true
            }

            val now = System.currentTimeMillis()
            if (now - lastMoveTime < moveCooldown) return true
            lastMoveTime = now

            val move = width / 3f
            carX = if (event.x < width / 2f) {
                carX - move
            } else {
                carX + move
            }

            carX = max(0f, min(carX, width - carWidth))
        }
        return true
    }

    private fun resetGame() {
        enemies.clear()
        carX = width / 2f - carWidth / 2f
        carY = height - carHeight - 60f
        speedBase = 12f
        spawnInterval = 800L
        score = 0
        gameRunning = true
        lastSpawnTime = System.currentTimeMillis()
    }
}
