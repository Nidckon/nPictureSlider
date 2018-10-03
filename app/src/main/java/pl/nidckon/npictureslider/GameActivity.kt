package pl.nidckon.npictureslider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_game.*
import java.util.*

class GameActivity : AppCompatActivity() {
    val log = Logger.get()
    private var puzzle: PuzzleManager? = null
    private var preview: RelativeLayout? = null
    private var counter: Int = 0
    private var isResolved: Boolean = false
    private var isReady: Boolean = false

    companion object {
        const val FIELD_PATH = "path"
        const val FIELD_URI = "uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        val path: String? = intent.getStringExtra(GameActivity.FIELD_PATH)
        val uri: String? = intent.getStringExtra(GameActivity.FIELD_URI)
        log.i(this, ">> path: $path")
        log.i(this, ">> uri: $uri")
        if (path == "") throw IllegalStateException("path is null!")
        val point = windowManager.defaultDisplay.getSquare()
        val bmp = createFromUriOrAssets(uri, path, assets)
                .fitToWindow(point.x, point.y)
        puzzle = PuzzleManager(bmp, findViewById(R.id.container))
        preview = findViewById(R.id.previewContainer)
        val previewImage = findViewById<ImageView>(R.id.preview)
        previewImage.setImageBitmap(bmp)
        isReady = true
        findViewById<RelativeLayout>(R.id.container).setOnClickListener {
            if (isReady) {
                isResolved = puzzle?.isResolved()?: false
                if (isResolved) {
                    showFull()
                }
            }
        }
    }

    private fun showFull() {
        isReady = false
        findViewById<RelativeLayout>(R.id.container)
                .visibility = View.INVISIBLE
        val preview = findViewById<RelativeLayout>(R.id.previewContainer)
        preview.findViewById<TextView>(R.id.previewText).visibility = View.GONE
        preview.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.view_full_image -> {
                log.i(this, "full image to vision")
                if (isReady) {
                    onShowHidePreviewAction()
                }
            }
            R.id.popup_numbers -> {
                log.i(this, "show-hide numbers")
                puzzle?.manageNumbers()
            }
        }
        return true
    }

    private fun onShowHidePreviewAction() {
        if (isResolved.not()) {
            if (Settings.getSettings().previewTime == -1) {
                showHideAction(counter == -1)
            } else {
                showHideTemporaryAction(counter > 0)
            }
        }
    }

    private fun showHideTemporaryAction(isVisible: Boolean = false) {
        if (isVisible) {
            log.i(this, "Show-Hide Temporary: isVisible[$isVisible] + operation: Hide")
            this.counter = 0
            preview?.visibility = View.INVISIBLE
        } else {
            val previewManager = ShowHidePreviewManager()
            this.counter = Settings.getSettings().previewTime
            log.i(this, "Show-Hide Temporary: isVisible[$isVisible] + counter[$counter] + operation: Show")
            previewManager.execute()
        }
    }

    private fun showHideAction(hide: Boolean = true) {
        if (hide) {
            log.i(this, "Show-Hide Action: want to hide[$hide], counter[$counter]")
            counter = 0
            preview?.visibility = View.INVISIBLE
        } else {
            log.i(this, "Show-Hide Action: want to hide[$hide], counter[$counter]")
            counter = -1
            preview?.visibility = View.VISIBLE
        }
    }

    private inner class ShowHidePreviewManager : AsyncTask<Any, Double, Boolean>() {
        private var lastToast: Toast? = null
        override fun onProgressUpdate(vararg values: Double?) {
            val showValue = values[0]?.toInt()?.plus(1)
            log.i(this, "Preview-Update: time[$showValue]")
            lastToast = Toast.makeText(baseContext, "${showValue}s", Toast.LENGTH_SHORT)
            lastToast?.show()
        }

        override fun onPreExecute() {
            log.i(this, "Preview-Pre: show Preview")
            preview?.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg params: Any?): Boolean? {
            var innerCounter = 0.0
            while (counter > 0) {
                if (innerCounter >= 1.0) {
                    innerCounter = 0.0
                    counter--
                    log.i(this, "Preview-Timer: 1s elapsed, counter[$counter]")
                }
                Thread.sleep(100)
                innerCounter += 0.1
                publishProgress(counter - innerCounter)
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            log.i(this, "Preview-Post: hide Preview")
            lastToast?.cancel()
            preview?.visibility = View.INVISIBLE
            counter = 0
        }
    }

    private inner class PuzzleManager(private val bmp: Bitmap, private val container: RelativeLayout) {
        private val images: Array<Array<PicturePart?>>
        val rows = Settings.getSettings().size
        val cols = Settings.getSettings().size

        init {
            log.i(this, "init")
            images = createCols()
            random()
            container.invalidate()
        }

        fun isResolved(): Boolean = images
                .map(this::isResolvedRow)
                .none { isRowGood -> !isRowGood }

        private fun isResolvedRow(row:Array<PicturePart?>): Boolean =
                row.map { picturePart -> picturePart?.isGoodPosition() ?: true }
                    .none { isPositionGood -> !isPositionGood }

        fun manageNumbers() = this.images.forEach { e -> e.forEach { img -> img?.manageNumber() } }

        private fun createCols(): Array<Array<PicturePart?>> = Array(cols) { createRows(it) }
        private fun createRows(c: Int): Array<PicturePart?> = Array(rows) { createImageView(c, it) }

        private fun createImageView(c: Int, r: Int): PicturePart? {
            if (c * r == (cols - 1) * (rows - 1)) return null
            val bitmap = bmp.cut(c, r, cols, rows)
            val imageView = PicturePart(baseContext, c, r, bitmap.width, bitmap.height)
            imageView.setImageBitmap(bitmap)
            container.addView(imageView)
            return imageView
        }

        private fun random() {
            val rand = Random()
            for (i in 0..rows * cols * rows) {
                val n = getNullPos()
                var c = n.x
                var r = n.y
                if (rand.nextBoolean()) {
                    if (rand.nextBoolean()) {
                        if (c + 1 < cols) c += 1
                        else c -= 1
                    } else {
                        if (c - 1 > -1) c -= 1
                        else c += 1
                    }
                } else {
                    if (rand.nextBoolean()) {
                        if (r + 1 < cols) r += 1
                        else r -= 1
                    } else {
                        if (r - 1 > -1) r -= 1
                        else r += 1
                    }
                }
                images[c][r]?.callOnClick()
            }
        }

        private fun getNullPos(): Point =
                images.withIndex().filter { it ->
                    !it.value.none { it == null }
                }.map { it ->
                    val c = it.index
                    it.value.withIndex().filter {
                        it.value == null
                    }.map {
                        val r = it.index
                        Point(c, r)
                    }.first()
                }.first()

        private inner class PicturePart(ctx: Context, private var c: Int, private var r: Int, val widthB: Int, val heightB: Int) : RelativeLayout(ctx) {
            private var imageView: ImageView? = null
            private var number: TextView? = null
            var numberVisibility: Boolean = false
            var original: Bitmap? = null

            init {
                inflate(baseContext, R.layout.image_entry, this)
                initContent()
                initPosition()
            }

            private fun initContent() {
                imageView = findViewById(R.id.image)
                number = findViewById(R.id.text)
                number?.text = "${c + r * rows}"
            }

            fun isGoodPosition(): Boolean = arrayOf(number?.text!!)
                    .any { text -> text == "${c + r * rows}" }

            private fun initPosition() {
                this.setOnClickListener {
                    when (true) {
                        canBeMovedDown() -> moveDown()
                        canBeMovedLeft() -> moveLeft()
                        canBeMovedRight() -> moveRight()
                        canBeMovedUp() -> moveUp()
                        canBeMovedInColDown() -> moveColDown()
                        canBeMovedInColUp() -> moveColUp()
                        canBeMovedInRowLeft() -> moveRowLeft()
                        canBeMovedInRowRight() -> moveRowRight()
                    }
                    val parentGroup = this as ViewGroup
                    val parentView = parentGroup.parent as RelativeLayout
                    parentView.callOnClick()

                }
                val params = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(widthB * c, heightB * r, 0, 0)
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

            private fun move(targetC: Int, targetR: Int, updateView: Boolean = true) {
                val picturePart = images[targetC][targetR]
                if (picturePart != null) {
                    log.i(this, "target [$targetC, $targetR] picture part is not a null!")
                    picturePart.switch(this)
                } else {
                    log.i(this, "target [$targetC, $targetR] is a null!")
                    images[targetC][targetR] = images[c][r]
                    images[c][r] = null
                    c = targetC
                    r = targetR
                }
                if (updateView)
                    changeVisiblePosition()
            }

            private fun switch(puzzle: PicturePart, updateView: Boolean = true) {
                log.i(this, "Init: [$puzzle] with [$this]")
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
                val forContent: (position: Int) -> Unit = { position ->
                    run {
                        val puzzle = images[this.c][position]
                        if (puzzle != null) {
                            if (puzzle.canBeMovedUp())
                                puzzle.move(puzzle.c, position - 1)
                            else if (puzzle.canBeMovedDown())
                                puzzle.move(puzzle.c, position + 1)
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
                val forContent: (position: Int) -> Unit = { position ->
                    run {
                        val puzzle = images[position][this.r]
                        if (puzzle != null) {
                            if (puzzle.canBeMovedLeft())
                                puzzle.move(position - 1, puzzle.r)
                            else if (puzzle.canBeMovedRight())
                                puzzle.move(position + 1, puzzle.r)
                        }
                    }
                }
                if (startPosition > 0) {
                    for (position in startPosition downTo this.c) {
                        forContent.invoke(position)
                    }
                } else {
                    for (position in startPosition..this.c) {
                        forContent.invoke(position)
                    }
                }
            }

            private fun changeVisiblePosition() {
                val params = (layoutParams as RelativeLayout.LayoutParams)
                params.setMargins(widthB * c, heightB * r, 0, 0)
                this.layoutParams = params
                this.invalidate()
                container.invalidate()
            }

            private fun moveUp() = move(c, r - 1)
            private fun moveDown() = move(c, r + 1)
            private fun moveLeft() = move(c - 1, r)
            private fun moveRight() = move(c + 1, r)

            fun canBeMovedUp(): Boolean = this.r - 1 > -1 && images[c][r - 1] == null
            fun canBeMovedDown(): Boolean = this.r + 1 < rows && images[c][r + 1] == null
            fun canBeMovedLeft(): Boolean = this.c - 1 > -1 && images[c - 1][r] == null
            fun canBeMovedRight(): Boolean = this.c + 1 < cols && images[c + 1][r] == null

            private fun moveColUp() = moveCol(0)
            private fun moveColDown() = moveCol(rows - 1)
            private fun moveRowLeft() = moveRow(0)
            private fun moveRowRight() = moveRow(cols - 1)

            fun canBeMovedInColUp(): Boolean =
                    this.r - 1 > -1 && images[this.c].filterIndexed { index, _ ->
                        index < this.r
                    }.any { e -> e == null }

            fun canBeMovedInColDown(): Boolean =
                    this.r + 1 < rows && images[this.c].filterIndexed { index, _ ->
                        index > this.r
                    }.any { e -> e == null }

            fun canBeMovedInRowLeft(): Boolean =
                    this.c - 1 > -1 && images.filterIndexed { index, _ ->
                        index < this.c
                    }.any { e -> e[this.r] == null }

            fun canBeMovedInRowRight(): Boolean =
                    this.c + 1 < cols && images.filterIndexed { index, _ ->
                        index > this.c
                    }.any { e -> e[this.r] == null }
        }
    }
}
