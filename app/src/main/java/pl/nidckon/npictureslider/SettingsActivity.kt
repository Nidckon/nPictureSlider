package pl.nidckon.npictureslider

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val size = Settings.getSettings().size
    private val min = Settings.getSettings().MIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        val spinner = findViewById<Spinner>(R.id.spinner)
        spinner.setSelection(size-min, false)
        spinner.invalidate()
        spinner.onItemSelectedListener = this
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val newSize = (min + position)
        Logger.get().i(this, "<< Settings-Size: $newSize")
        Settings.getSettings().size = newSize
        getSharedPreferences("pictureslider", 0)
                .edit()
                .putInt("size", newSize)
                .apply()
    }
}
