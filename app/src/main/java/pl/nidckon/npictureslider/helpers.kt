package pl.nidckon.npictureslider

import android.content.Context
import android.content.CursorLoader
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.annotation.LayoutRes
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.io.File

// to inflate in BaseAdapter
fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

// create Bitmap from assets
fun createFromUriOrAssets(uri: String?, path: String?, assets: AssetManager): Bitmap =
        if (uri != null) createFromUri(uri) else createFromAssets(path!!, assets)

fun createFromAssets(path: String, assets: AssetManager): Bitmap {
    val stream = assets.open(path)
    return BitmapFactory.decodeStream(stream)
}

fun createFromUri(uriPath: String): Bitmap {
    val file = File(uriPath)
    val inputStream = file.inputStream()
    val bmp = BitmapFactory.decodeStream(inputStream)
    inputStream.close()
    return bmp
}

fun Display.getSquare(): Point {
    val point = Point()
    this.getSize(point)
    Logger.get().d(this, "Display-Size: [${point.x} x ${point.y}]")
    if (point.x > point.y) point.x = point.y
    else if (point.y > point.x) point.y = point.x
    Logger.get().d(this, "Display-Square-Size: [${point.x} x ${point.y}]")
    return point
}

// to fit image in window
fun Bitmap.fitToWindow(widthWin: Int, heightWin: Int): Bitmap {
    var width = this.width
    var height = this.height
    Logger.get().d(this, "Image-Size: [${width} x ${height}]")
    if (width > widthWin) {
        val scale = width / widthWin
        width = widthWin
        height /= scale
    }
    if (height > heightWin) {
        val scale = height / heightWin
        height = heightWin
        width /= scale
    }
    Logger.get().d(this, "Image-Size-Scaled: [${width} x ${height}]")
    return Bitmap.createScaledBitmap(this, width, height, false)
}

// to cut concrete fragment from image
fun Bitmap.cut(col: Int, row: Int, COLS: Int, ROWS: Int): Bitmap {
    Logger.get().d(this, ">> init with params {col: $col; row: $row; COLS: $COLS; ROWS: $ROWS}")
    val width = this.width / COLS
    val height = this.height / ROWS
    Logger.get().d(this, "custom size: [$width x $height]")
    val startWidth = width * if (col > COLS) COLS - 1 else col
    val startHeight = height * if (row > ROWS) ROWS - 1 else row
    Logger.get().d(this, "custom start-size: [$startWidth x $startHeight]")
    return Bitmap.createBitmap(this, startWidth, startHeight, width, height)
}

fun Bitmap.cut(position: Int, COLS: Int, ROWS: Int): Bitmap {
    val parts = COLS * ROWS
    val col = if (parts > position) parts % COLS else COLS - 1
    val row = if (parts > position) parts / ROWS else ROWS - 1
    return this.cut(col, row, COLS, ROWS)
}

//to get PATH to element from file-chooser
fun Uri.getPath(ctx: Context): String {
    var truePath = ""
    val cols = arrayOf(MediaStore.Images.Media.DATA)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val wholeID = DocumentsContract.getDocumentId(this)
        val ID = wholeID.split(":")[1]
        val selected = MediaStore.Images.Media._ID + "=?"
        val cursor = ctx.contentResolver
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cols, selected, arrayOf(ID), null)
        val colID = cursor.getColumnIndex(cols[0])
        if (cursor.moveToFirst()) {
            truePath = cursor.getString(colID)
        }
        cursor.close()
    } else {  // older API
        val cursorLoader = CursorLoader(ctx, this, cols, null, null, null)
        val cursor = cursorLoader.loadInBackground()
        if (cursor != null) {
            val colId = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            truePath = cursor.getString(colId)
            cursor.close()
        }
    }
    return truePath
}
