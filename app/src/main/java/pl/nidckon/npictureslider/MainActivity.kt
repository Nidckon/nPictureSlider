package pl.nidckon.npictureslider

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : AppCompatActivity() {
    private val log = Logger.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("pictureslider", 0)
        setContentView(R.layout.activity_main)
        log.i(this, "<< onCreate")
        Settings.getSettings().size = preferences.getInt("size", Settings.getSettings().size)
        log.i(this, "<< Settings-Size: ${Settings.getSettings().size}")
        initGallery()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.settings -> {
                val settingsIntent = Intent(baseContext, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.id.report_bug -> {
                val emailIntent = Logger.get().prepareEmail(baseContext)
                startActivity(Intent.createChooser(emailIntent, "Wyślij raport"))
            }
        }
        return true
    }


    private fun initGallery(){
        val galleryList = getAssetsGalleryList()
        log.i(this, "<< GalleryList-Size: ${galleryList.size}")
        val container = findViewById<ListView>(R.id.container)
        val adapter = GalleryAdapter(galleryList)
        container.adapter = adapter
        container.setOnItemClickListener { parent, _, position, _ -> onGalleryItemClick(parent, position) }
        container.invalidate()
    }

    private fun getAssetsGalleryList():List<String>{
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

    private inner class GalleryAdapter(private val paths:List<String>) : BaseAdapter(){
        private val list:Map<String, Bitmap> = HashMap()

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
