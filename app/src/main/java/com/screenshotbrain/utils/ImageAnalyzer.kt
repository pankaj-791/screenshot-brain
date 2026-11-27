package com.screenshotbrain.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.security.MessageDigest

data class ImageAnalysis(
    val contentHash: String,
    val visualHash: Long?,
    val sharpness: Double?
)

object ImageAnalyzer {

    private const val TARGET_SIDE = 512
    private const val BLUR_SIDE = 256

    /** Returns null if the image cannot be opened at all. */
    fun analyze(context: Context, uri: Uri): ImageAnalysis? {
        val hash = contentHash(context, uri) ?: return null
        val bitmap = decodeSmall(context, uri) ?: return ImageAnalysis(hash, null, null)
        return try {
            ImageAnalysis(hash, differenceHash(bitmap), sharpness(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    /** Number of differing bits between two look-alike fingerprints (0 = identical). */
    fun distance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun contentHash(context: Context, uri: Uri): String? =
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }

    private fun decodeSmall(context: Context, uri: Uri): Bitmap? =
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) {
                null
            } else {
                var sample = 1
                while (longest / (sample * 2) >= TARGET_SIDE) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }

    private fun gray(pixel: Int): Double =
        0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)

    private fun differenceHash(source: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(source, 9, 8, true)
        try {
            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val left = gray(small.getPixel(x, y))
                    val right = gray(small.getPixel(x + 1, y))
                    hash = (hash shl 1) or (if (left > right) 1L else 0L)
                }
            }
            return hash
        } finally {
            if (small !== source) small.recycle()
        }
    }

    private fun sharpness(source: Bitmap): Double {
        val scale = minOf(1.0, BLUR_SIDE.toDouble() / maxOf(source.width, source.height))
        val w = maxOf(3, (source.width * scale).toInt())
        val h = maxOf(3, (source.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)
        try {
            val pixels = IntArray(w * h)
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)
            val g = DoubleArray(w * h) { gray(pixels[it]) }
            var sum = 0.0
            var sumSquares = 0.0
            var count = 0
            for (y in 1 until h - 1) {
                for (x in 1 until w - 1) {
                    val laplacian = 4 * g[y * w + x] - g[(y - 1) * w + x] -
                        g[(y + 1) * w + x] - g[y * w + x - 1] - g[y * w + x + 1]
                    sum += laplacian
                    sumSquares += laplacian * laplacian
                    count++
                }
            }
            if (count == 0) return 0.0
            val mean = sum / count
            return sumSquares / count - mean * mean
        } finally {
            if (scaled !== source) scaled.recycle()
        }
    }
}
