package com.wikzo.todo.ui.pairing

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Renders [content] as a square black-on-white QR code bitmap, [sizePx] pixels on a
 * side. Deliberately plain black/white regardless of the app's light/dark theme --
 * QR scanners rely on that contrast, so this shouldn't follow dark mode.
 */
fun generateQrCodeBitmap(content: String, sizePx: Int): Bitmap {
    val bitMatrix: BitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
    for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) BLACK else WHITE)
        }
    }
    return bitmap
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()
