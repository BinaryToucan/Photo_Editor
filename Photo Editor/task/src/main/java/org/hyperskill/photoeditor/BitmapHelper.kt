package org.hyperskill.photoeditor

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.set
import kotlin.math.pow

typealias BitmapStep = (Bitmap) -> Bitmap

/**
 * Функции-помощники при работе с Bitmap
 */
object BitmapHelper {

    /**
     * Применение яркости
     */
    fun Bitmap.applyBrightness(filter: Double): Bitmap {
        val rangeColor = 0..255
        val height = this.height
        val width = this.width

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val filterInt = filter.toInt()
        for(y in 0 until height){
            for(x in 0 until width){
                val pixelColor = this[x, y]
                val alpha: Int = Color.alpha(pixelColor)
                val red: Int = (Color.red(pixelColor) + filterInt).coerceIn(rangeColor)
                val green: Int = (Color.green(pixelColor) + filterInt).coerceIn(rangeColor)
                val blue: Int = (Color.blue(pixelColor) + filterInt).coerceIn(rangeColor)

                val newColor: Int = Color.argb(alpha, red, green, blue)
                bitmap[x, y] = newColor
            }
        }

        return bitmap
    }

    /**
     * Применение контраста
     */
    fun Bitmap.applyContrast(filter: Double): Bitmap {
        val rangeColor = 0..255
        val height = this.height
        val width = this.width

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        var totalBright = 0L
        for(y in 0 until height){
            for(x in 0 until width){
                val pixelColor = this[x, y]
                val red: Int = Color.red(pixelColor).coerceIn(rangeColor)
                val green: Int = Color.green(pixelColor).coerceIn(rangeColor)
                val blue: Int = Color.blue(pixelColor).coerceIn(rangeColor)

                totalBright += red + green + blue
            }
        }

        val avgBright = (totalBright / (height * width * 3)).toInt()
        val alpha = (255 + filter)/(255 - filter)
        for(y in 0 until height){
            for(x in 0 until width){
                val pixelColor = this[x, y]
                val alphaPixel: Int = Color.alpha(pixelColor)
                val red: Int = (alpha * (Color.red(pixelColor) - avgBright) + avgBright).toInt().coerceIn(rangeColor)
                val green: Int = (alpha * (Color.green(pixelColor) - avgBright) + avgBright).toInt().coerceIn(rangeColor)
                val blue: Int = (alpha * (Color.blue(pixelColor) - avgBright) + avgBright).toInt().coerceIn(rangeColor)

                val newColor: Int = Color.argb(alphaPixel, red, green, blue)
                bitmap[x, y] = newColor
            }
        }

        return bitmap
    }

    /**
     * Применение насыщенность-фильтра
     */
    fun Bitmap.applySaturation(filter: Double): Bitmap {
        val rangeColor = 0..255
        val height = this.height
        val width = this.width

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val alpha: Double = (255 + filter) / (255 - filter)
        for(y in 0 until height){
            for(x in 0 until width){
                val pixelColor = this[x, y]
                val alphaPixel: Int = Color.alpha(pixelColor)
                val rgbAvg = (Color.red(pixelColor) + Color.green(pixelColor) + Color.blue(pixelColor)) / 3

                val red: Int = (alpha * (Color.red(pixelColor) - rgbAvg) + rgbAvg).toInt().coerceIn(rangeColor)
                val green: Int = (alpha * (Color.green(pixelColor) - rgbAvg) + rgbAvg).toInt().coerceIn(rangeColor)
                val blue: Int = (alpha * (Color.blue(pixelColor)  - rgbAvg) + rgbAvg).toInt().coerceIn(rangeColor)

                val newColor: Int = Color.argb(alphaPixel, red, green, blue)
                bitmap[x, y] = newColor
            }
        }

        return bitmap
    }

    /**
     * Применение гамма-фильтра
     */
    fun Bitmap.applyGamma(filter: Double): Bitmap {
        val rangeColor = 0..255
        val height = this.height
        val width = this.width

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        for(y in 0 until height){
            for(x in 0 until width){
                val pixelColor = this[x, y]
                val alphaPixel: Int = Color.alpha(pixelColor)
                val red: Int = (255 * (Color.red(pixelColor) / 255.0).pow(filter)).toInt().coerceIn(rangeColor)
                val green: Int = (255 * (Color.green(pixelColor) / 255.0).pow(filter)).toInt().coerceIn(rangeColor)
                val blue: Int = (255 * (Color.blue(pixelColor) / 255.0).pow(filter)).toInt().coerceIn(rangeColor)

                val newColor: Int = Color.argb(alphaPixel, red, green, blue)
                bitmap[x, y] = newColor
            }
        }

        return bitmap
    }

    /**
     * Создание дефолтного изображения
     */
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)
        // get pixel array from source

        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x+y) % 100 + 120

                pixels[index] = Color.rgb(R,G,B)

            }
        }
        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }
}

