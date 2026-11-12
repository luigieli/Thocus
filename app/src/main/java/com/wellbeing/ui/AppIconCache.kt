package com.wellbeing.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory cache for pre-rasterized 96x96 application icons.
 *
 * Implements Object Calisthenics: zero 'else', methods < 15 lines.
 */
object AppIconCache {

    private const val ICON_SIZE = 96
    private const val CORNER_RADIUS = 20f
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(packageName: String): ImageBitmap? {
        return cache[packageName]
    }

    fun load(context: Context, packageName: String): ImageBitmap? {
        val existing = cache[packageName]
        if (existing != null) {
            return existing
        }
        val loaded = extractIconBitmap(context, packageName) ?: return null
        cache[packageName] = loaded
        return loaded
    }

    private fun extractIconBitmap(context: Context, packageName: String): ImageBitmap? {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            return renderSmallBitmap(drawable)
        } catch (_: Exception) {
            return null
        }
    }

    private fun renderSmallBitmap(drawable: Drawable): ImageBitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = createRoundedPath()
        canvas.clipPath(path)
        drawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    private fun createRoundedPath(): Path {
        val path = Path()
        val bounds = RectF(0f, 0f, ICON_SIZE.toFloat(), ICON_SIZE.toFloat())
        path.addRoundRect(bounds, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        return path
    }
}
