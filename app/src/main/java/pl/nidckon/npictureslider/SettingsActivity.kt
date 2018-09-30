package pl.nidckon.npictureslider

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import pl.nidckon.npictureslider.Settings.Companion.const

class SettingsActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    private val size = Settings.getSettings().size
    private val min = Settings.getSettings().MIN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        initSize()
        initTime()
    }

    private fun initSize() {
        val spinner = findViewById<Spinner>(R.id.partsAmount)
        spinner.setSelection(size-min, false)
        spinner.invalidate()
        spinner.onItemSelectedListener = this
    }

    private fun initTime() {
        val spinner = findViewById<Spinner>(R.id.previewTime)
        spinner.setSelection(getTimePositionByValue(Settings.getSettings().previewTime), false)
        spinner.invalidate()
        spinner.onItemSelectedListener = this
    }

    private fun getTimePositionByValue(value:Int): Int {
        Logger.get().i(this,"resources string array: ${resources.getStringArray(R.array.preview_time).toList()}")
        Logger.get().i(this,"resources string array values: ${resources.getStringArray(R.array.preview_time_values).toList()}")
        return resources.getStringArray(R.array.preview_time_values)
                .mapIndexed { index, v -> if (v.toInt()==value) index else null }
                .first { v -> v != null }?: 0
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        Logger.get().i(this, "Catch event: [position= $position, id= $id, parent= ${parent?.id}, view= ${view?.id?.toLong()}]")
        Logger.get().i(this, "Event when TIME[${R.id.previewTime}] and SIZE[${R.id.partsAmount}]")
        when(parent?.id) {
            R.id.partsAmount -> onPicturePartsChange(position)
            R.id.previewTime -> onPreviewTimeChange(position)
        }
    }

    private fun onPreviewTimeChange(position: Int) {
        val times = resources.getStringArray(R.array.preview_time_values)
        val newVal = times[position].toInt()
        Logger.get().i(this, "<< Settings-Time: $newVal")
        Settings.getSettings().previewTime = newVal
        getSharedPreferences(const.PREF_NAME, 0)
                .edit()
                .putInt(const.TIME, newVal)
                .apply()
    }

    private fun onPicturePartsChange(position: Int) {
        val newSize = (min + position)
        Logger.get().i(this, "<< Settings-Size: $newSize")
        Settings.getSettings().size = newSize
        getSharedPreferences(const.PREF_NAME, 0)
                .edit()
                .putInt(const.SIZE, newSize)
                .apply()
    }
}
