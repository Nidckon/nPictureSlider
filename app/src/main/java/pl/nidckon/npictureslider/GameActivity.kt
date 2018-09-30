package pl.nidckon.npictureslider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import java.util.*

class GameActivity : AppCompatActivity() {
    val log = Logger.get()
    private var puzzle: PuzzleManager? = null

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
                log.i(this, "show-hide numbers")
                puzzle?.manageNumbers()
            }
        }
        return true
    }

    private inner class PuzzleManager(private val bmp:Bitmap, private val container:RelativeLayout) {
        private val images: Array<Array<PicturePart?>>
        val ROWS = Settings.getSettings().size
        val COLS = Settings.getSettings().size

        init {
            log.i(this, "init")
            images = createCols()
            random()
            container.invalidate()
        }

        fun manageNumbers() = this.images.forEach { e -> e.forEach { img -> img?.manageNumber() } }

        private fun createCols(): Array<Array<PicturePart?>> = Array(COLS) {createRows(it)}
        private fun createRows(c: Int): Array<PicturePart?> = Array(ROWS) {createImageView(c, it)}

        private fun createImageView(c: Int, r: Int):PicturePart? {
            if (c*r == (COLS-1)*(ROWS-1)) return null
            val bitmap = bmp.cut(c, r, COLS, ROWS)
            val imageView = PicturePart(baseContext, c, r, bitmap.width, bitmap.height)
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

        private inner class PicturePart(private val ctx: Context, private var c:Int, private var r:Int, val widthB:Int, val heightB:Int): RelativeLayout(ctx){
            private var imageView: ImageView? = null
            private var number: TextView? = null
            var numberVisibility: Boolean = false
            var original: Bitmap? = null

            init {
                inflate(getContext(), R.layout.image_entry, this)
                initContent()
                initPosition()
            }

            private fun initContent() {
                imageView = findViewById<ImageView>(R.id.image)
                number = findViewById<TextView>(R.id.text)
                val no = c * COLS + r
                number?.text = "$no"
            }

            private fun initPosition() {
                this.setOnClickListener {
                    when(true){
                        canBeMovedDown() -> moveDown()
                        canBeMovedLeft() -> moveLeft()
                        canBeMovedRight() -> moveRight()
                        canBeMovedUp() -> moveUp()
                        canBeMovedInColDown() -> moveColDown()
                        canBeMovedInColUp() -> moveColUp()
                        canBeMovedInRowLeft() -> moveRowLeft()
                        canBeMovedInRowRight() -> moveRowRight()
                    }
                }
                val params = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(widthB*c, heightB*r,0,0)
                layoutParams = params
            }

            fun setImageBitmap(bmp: Bitmap?) {
                original = bmp
                imageView?.setImageBitmap(bmp)
            }

            fun manageNumber() {
                numberVisibility = numberVisibility.not()
                if (numberVisibility) {
                    number?.visibility = View.VISIBLE
                } else {
                    number?.visibility = View.INVISIBLE
                }
            }

            private fun move(targetC:Int, targetR:Int, updateView: Boolean = true){
                val picturePart = images[targetC][targetR]
                if (picturePart != null) {
                    log.i(this,"target [$targetC, $targetR] picture part is not a null!")
                    picturePart.switch(this)
                } else {
                    log.i(this,"target [$targetC, $targetR] is a null!")
                    images[targetC][targetR] = images[c][r]
                    images[c][r] = null
                    c = targetC
                    r = targetR
                }
                if (updateView)
                    changeVisiblePosition()
            }

            private fun switch(puzzle: PicturePart, updateView: Boolean = true) {
                log.i(this,"Init: [$puzzle] with [$this]")
                val tempC = puzzle.c
                val tempR = puzzle.r
                puzzle.c = this.c
                puzzle.r = this.r
                this.c = tempC
                this.r = tempR
                images[c][r] = this
                images[puzzle.c][puzzle.r] = puzzle
                if (updateView)
                    this.changeVisiblePosition()
            }

            private fun moveCol(startPosition: Int) {
                log.i(this, "Move in Column[${this.c}]: $startPosition..${this.r}")
                val forContent: (position:Int) -> Unit = { position ->
                    run {
                        val puzzle = images[this.c][position]
                        if (puzzle != null) {
                            if (puzzle.canBeMovedUp())
                                puzzle.move(puzzle.c, position - 1)
                            else if (puzzle.canBeMovedDown())
                                puzzle.move(puzzle.c, position+1)
                        }
                    }
                }
                if (startPosition > 0) {
                    for (position in startPosition downTo this.r) {
                        forContent.invoke(position)
                    }
                } else {
                    for (position in startPosition..this.r) {
                        forContent.invoke(position)
                    }
                }
            }

            private fun moveRow(startPosition: Int) {
                log.i(this, "Move in Row[${this.r}]: $startPosition..${this.c}")
//                for (position in startPosition downTo  this.c) {
//                    val puzzle = images[this.c][position]
//                    if (puzzle != null) {
//                        if (puzzle.canBeMovedLeft())
//                            puzzle.move(position - 1, this.r)
//                        else if (puzzle.canBeMovedRight())
//                            puzzle.move(position + 1, this.r)
//                        if (puzzle.canBeMovedDown())
//                            log.i(this, "Nani?! It can be moved down!")
//                        if (puzzle.canBeMovedUp())
//                            log.i(this, "Nani?! It can be moved up!")
//                    }
//                }
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

            private fun moveColUp() = moveCol(0)
            private fun moveColDown() = moveCol(ROWS-1)
            private fun moveRowLeft() = moveRow(0)
            private fun moveRowRight() = moveRow(COLS-1)

            fun canBeMovedInColUp(): Boolean =
                    this.r-1 > -1 && images[this.c].filterIndexed { index,_ -> index < this.r
                    }.any { e -> e == null }
            fun canBeMovedInColDown(): Boolean =
                    this.r+1 < ROWS && images[this.c].filterIndexed { index,_ -> index > this.r
                    }.any { e -> e == null }
            fun canBeMovedInRowLeft(): Boolean =
                    this.c-1 > -1 && images.filterIndexed { index,_ -> index < this.c
                    }.any { e -> e[this.r] == null }
            fun canBeMovedInRowRight(): Boolean =
                    this.c+1 < COLS && images.filterIndexed { index,_ -> index > this.c
                    }.any { e -> e[this.r] == null }
        }
    }
}
