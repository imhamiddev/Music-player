package com.imhamiddev.musicplayer.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import androidx.media3.common.util.BitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Loads the cover shown in the notification and on the lock screen. Songs with no cover get a dark
 * placeholder, so the system always picks a dark background and light text that can be read.
 */
class ThumbBitmapLoader(private val ctx: Context) : BitmapLoader {
    private val executor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit(Callable { BitmapFactory.decodeByteArray(data, 0, data.size) ?: placeholder() })

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        executor.submit(Callable { load(uri) })

    private fun load(uri: Uri): Bitmap {
        val thumb = runCatching {
            if (Build.VERSION.SDK_INT >= 29) ctx.contentResolver.loadThumbnail(uri, Size(512, 512), null) else null
        }.getOrNull()
        return thumb ?: placeholder()
    }

    private fun placeholder(): Bitmap {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), 0xFF26262C.toInt(), 0xFF111113.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        // A simple record: green ring, dark centre.
        paint.color = 0xFF22C55E.toInt()
        c.drawCircle(size / 2f, size / 2f, size * 0.30f, paint)
        paint.color = 0xFF1B1B1F.toInt()
        c.drawCircle(size / 2f, size / 2f, size * 0.22f, paint)
        paint.color = 0xFF22C55E.toInt()
        c.drawCircle(size / 2f, size / 2f, size * 0.06f, paint)
        return bmp
    }
}

/**
 * The normal media notification, with a dark colour so the title and buttons stay readable on
 * systems that colour the notification (or the lock-screen player) from it.
 */
class ReadableNotificationProvider(ctx: Context) : MediaNotification.Provider {
    private val inner = DefaultMediaNotificationProvider(ctx)

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val n = inner.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        n.notification.color = 0xFF16161A.toInt()
        n.notification.extras.putBoolean("android.colorized", true)
        return n
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        inner.handleCustomCommand(session, action, extras)
}
