package pl.nidckon.npictureslider

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import pl.nidckon.npictureslider.Settings.Companion.const
import java.util.*

class MainActivity : AppCompatActivity() {
    private val log = Logger.get()

    companion object {
        private val SELECT_IMAGE = 15
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        log.i(this, "<< onCreate")
        collectSettings()
        initGallery()
    }

    private fun collectSettings() {
        val preferences = getSharedPreferences(const.PREF_NAME, 0)
        Settings.getSettings().size = preferences.getInt(const.SIZE, Settings.getSettings().size)
        log.i(this, "<< Settings-Size: ${Settings.getSettings().size}")
        Settings.getSettings().previewTime = preferences.getInt(const.TIME, Settings.getSettings().previewTime)
        log.i(this, "<< Settings-Time: ${Settings.getSettings().previewTime}")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                val settingsIntent = Intent(baseContext, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.id.report_bug -> {
                val emailIntent = Logger.get().prepareEmail(baseContext)
                startActivity(Intent.createChooser(emailIntent, "Wyślij raport"))
            }
            R.id.imageChooser -> {
                log.i(this, "invoke ImageChooser")
                onImageChoose()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onImageChoose() {
        val newIntent = Intent(Intent.ACTION_GET_CONTENT)
        newIntent.setType("image/*")
        newIntent.addCategory(Intent.CATEGORY_OPENABLE)
        if (newIntent.resolveActivity(packageManager) != null) {
            log.i(this, "ImageChooser - invoke")
            startActivityForResult(newIntent, SELECT_IMAGE)
        } else {
            log.i(this, "intent resolve activity null")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        log.i(this, "on result: ${Arrays.asList(requestCode, resultCode, data)}")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MainActivity.SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data?.data != null) {
                val uri = data.data
                val newIntent = Intent(baseContext, GameActivity::class.java)
                newIntent.putExtra(GameActivity.FIELD_URI, uri.getPath(baseContext))
                startActivity(newIntent)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun initGallery() {
        val galleryList = getAssetsGalleryList()
        log.i(this, "<< GalleryList-Size: ${galleryList.size}")
        val container = findViewById<ListView>(R.id.container)
        val adapter = GalleryAdapter(galleryList)
        container.adapter = adapter
        container.setOnItemClickListener { parent, _, position, _ -> onGalleryItemClick(parent, position) }
        container.invalidate()
    }

    private fun getAssetsGalleryList(): List<String> {
        val path = "gallery"
        val list = assets.list(path)
        return list.map { name -> "$path/$name" }
    }

    private fun onGalleryItemClick(parent: AdapterView<*>, position: Int) {
        log.d(this, "clicked on $position")
        Log.i("nPS:${this::javaClass.name}", "Click on $position")
        val path = (parent.adapter as GalleryAdapter).getItemKey(position)
        log.d(this, "__Sensitive - path: $path")
        Log.i("nPS:${this::javaClass.name}", "Have object: $path")
        var intent = Intent(baseContext, GameActivity::class.java)
        intent.putExtra(GameActivity.FIELD_PATH, path)
        startActivity(intent)
    }

    private inner class GalleryAdapter(private val paths: List<String>) : BaseAdapter() {
        private val list: Map<String, Bitmap> = HashMap()

        init {
            paths.forEach { imagePath -> createEntry(imagePath) }
        }

        private fun createEntry(imagePath: String) {
            val point = windowManager.defaultDisplay.getSquare()
            val bitmap = createFromAssets(imagePath, assets)
                    .fitToWindow(point.x, point.y)
            (list as HashMap)[imagePath] = bitmap
        }

        override fun getItem(position: Int): Bitmap? = if (paths.size > position) list[paths[position]] else null
        override fun getItemId(position: Int): Long = if (paths.size > position) position.toLong() else -1
        fun getItemKey(position: Int): String? = if (paths.size > position) paths[position] else ""
        override fun getCount(): Int = list.size

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View = convertView ?: parent.inflate(R.layout.image_entry)

            val img = getItem(position)
            val target = view.findViewById<ImageView>(R.id.image)
            target.setImageBitmap(img)

            return view
        }
    }
}
