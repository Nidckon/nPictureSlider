package pl.nidckon.npictureslider

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import java.util.*

class GameActivity : AppCompatActivity() {
    val log = Logger.get()
    var puzzle: PuzzleManager? = null

    companion object {
        val FIELD_PATH = "path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val path: String = intent.getStringExtra("path")
        log.i(this, ">> path: $path")
        if (path == "") throw IllegalStateException("path is null!")
        val point = windowManager.defaultDisplay.getSquare()
        val bmp = createFromAssets(path, assets)
                .fitToWindow(point.x, point.y)
        puzzle = PuzzleManager(bmp, findViewById(R.id.container))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.view_full_image -> {
                Toast.makeText(this, "Pokaż pełny obraz", Toast.LENGTH_LONG).show()
                log.i(this, "full image to vision")
            }
            R.id.popup_numbers -> {
                Toast.makeText(this, "Ustaw liczby na kawałkach", Toast.LENGTH_LONG).show()
                log.i(this, "show-hide numbers")
                puzzle?.manageNumbers()
            }
        }
        return true
    }

    private inner class PuzzleManager(private val bmp:Bitmap, private val container:RelativeLayout) {
        private val images: Array<Array<CustomImageView?>>
        val ROWS = Settings.getSettings().size
        val COLS = Settings.getSettings().size

        init {
            log.i(this, "init")
            images = createCols()
            random()
            container.invalidate()
        }

        fun manageNumbers() = this.images.forEach { e -> e.forEach { img -> img?.manageNumber() } }

        private fun createCols(): Array<Array<CustomImageView?>> = Array(COLS) {createRows(it)}
        private fun createRows(c: Int): Array<CustomImageView?> = Array(ROWS) {createImageView(c, it)}

        private fun createImageView(c: Int, r: Int):CustomImageView? {
            if (c*r == (COLS-1)*(ROWS-1)) return null
            val bitmap = bmp.cut(c, r, COLS, ROWS)
            val imageView = CustomImageView(baseContext, c, r, bitmap.width, bitmap.height)
            imageView.setImageBitmap(bitmap)
            container.addView(imageView)
            return imageView
        }

        private fun random(){
            val rand = Random()
            for (i in 0..ROWS*COLS*ROWS){
                val n = getNullPos()
                var _c = n.x
                var _r = n.y
                if (rand.nextBoolean()) {
                    if (rand.nextBoolean()){
                        if (_c + 1 < COLS) _c += 1
                        else _c -= 1
                    } else {
                        if (_c - 1 > -1) _c -= 1
                        else _c += 1
                    }
                } else {
                    if (rand.nextBoolean()){
                        if (_r + 1 < COLS) _r += 1
                        else _r -= 1
                    } else {
                        if (_r - 1 > -1) _r -= 1
                        else _r += 1
                    }
                }
                images[_c][_r]?.callOnClick()
            }
        }

        private fun getNullPos(): Point =
            images.withIndex().filter {
                !it.value.filter { it == null }.isEmpty()
            }.map {
                val _c = it.index
                it.value.withIndex().filter {
                    it.value == null
                }.map {
                    val _r = it.index
                    Point(_c, _r)
                }.first()
            }.first()

        private inner class CustomImageViewContainer(private )

        private inner class CustomImageView(private val ctx: Context, private var c:Int, private var r:Int, val widthB:Int, val heightB:Int) : ImageView(ctx){
            var number: Boolean = false
            var original: Bitmap? = null

            init {
                this.setOnClickListener {
                    when(true){
                        canBeMovedDown() -> moveDown()
                        canBeMovedLeft() -> moveLeft()
                        canBeMovedRight() -> moveRight()
                        canBeMovedUp() -> moveUp()
                    }
                }

                val params = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(widthB*c, heightB*r,0,0)
                layoutParams = params
            }

            fun manageNumber() {
                number.not()
                //TODO: show / hide
                if (number) {
                    val no = c * COLS + r
                    log.i(this, "element {$r, $c} = $no and is visible")
                } else {
                    log.i(this, "element is invisible")
                }
            }

            override fun setImageBitmap(bmp: Bitmap?) {
                original = bmp
                super.setImageBitmap(bmp)
            }

            private fun hideNumber() = super.setImageBitmap(original)

            private fun showNumber() {

            }

            private fun move(targetC:Int, targetR:Int){
                images[targetC][targetR] = images[c][r]
                images[c][r] = null
                c = targetC
                r = targetR
                changeVisiblePosition()
            }

            private fun changeVisiblePosition() {
                val params = (layoutParams as RelativeLayout.LayoutParams)
                params.setMargins(widthB * c, heightB * r, 0, 0)
                this.layoutParams = params
                this.invalidate()
                container.invalidate()
            }

            private fun moveUp() = move(c, r-1)
            private fun moveDown() = move(c, r+1)
            private fun moveLeft() = move(c-1, r)
            private fun moveRight() = move(c+1, r)

            fun canBeMovedUp(): Boolean = this.r-1 > -1 && images[c][r-1] == null
            fun canBeMovedDown(): Boolean = this.r+1 < ROWS && images[c][r+1] == null
            fun canBeMovedLeft(): Boolean = this.c-1 > -1 && images[c-1][r] == null
            fun canBeMovedRight(): Boolean = this.c+1 < COLS && images[c+1][r] == null
        }
    }
}
