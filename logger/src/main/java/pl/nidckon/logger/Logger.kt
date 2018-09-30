package pl.nidckon.logger

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class Logger private constructor(private var name: String) {
    init {
        name = "LOG:$name"
    }

    companion object {
        private var post: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        private val loggers = HashMap<String, Logger>()

        fun createLogger(url: String): Logger {
            if (!loggers.containsKey(url)) {
                loggers.put(url, Logger(url))
            }
            return loggers[url]!!
        }
    }

    fun i(txt: String) {
        Log.i(name, txt)
    }

    fun d(txt: String) {
        Log.d(name, txt)
    }

    fun e(txt: String) {
        Log.e(name, txt)
    }
}
