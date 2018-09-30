package pl.nidckon.npictureslider

import android.content.Context
import android.content.Intent
import android.util.Log

class Logger private constructor(){
    private val log = StringBuilder()

    companion object {
        private val logger = Logger()
        private val version = BuildConfig.VERSION_NAME
        private val release = "alpha"

        fun get() = logger
    }

    private fun formattedLine(what:String, msg:String):String = "nPS:${formatExecuter(what)}: $msg"

    private fun formatExecuter(executer: String): String {
        val list = executer.split(".")
        val result = StringBuilder()
        list.forEachIndexed { index, s ->
            if (index != list.lastIndex) {
                result.append(s.first())
                result.append(".")
            } else
                result.append(s)
        }
        return result.toString()
    }

    fun d(executer:Any, msg: String) {
        log.appendln(formattedLine(executer::class.java.name, msg))
        Log.d("nPS:${formatExecuter(executer::class.java.name)}", msg)
    }

    fun i(executer: Any, msg: String) {
            log.appendln(formattedLine(executer::class.java.name, msg))
        Log.i("nPS:${formatExecuter(executer::class.java.name)}", msg)
    }

    fun prepareEmail(ctx: Context): Intent{
        val separator = "\r\n--------------------------------\r\n"
        val type = "text/plain"
        val device_info = getDeviceInfo(ctx)
        val email_content = StringBuilder("Bug report for nPictureSlider, look at attachments...")

        val file1 = log.toString()
        val file2 = device_info

        email_content.append(separator)
        email_content.append(file1)
        email_content.append(separator)
        email_content.append(file2)

        val intent = Intent(Intent.ACTION_SEND)
        val programmer = arrayOf("nidckon@gmail.com", "patryk.jackowski5@wp.pl")
        intent.type = type
        intent.putExtra(Intent.EXTRA_EMAIL, programmer)
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug report - nPictureSlider")
        intent.putExtra(Intent.EXTRA_TEXT, email_content.toString())
        return intent
    }

    private fun getDeviceInfo(ctx:Context) = """
            Linux-OS: ${System.getProperty("os.version")}
            --
            CodeName: ${android.os.Build.VERSION.CODENAME}
            SDK: ${android.os.Build.VERSION.SDK_INT}
            Release: ${android.os.Build.VERSION.RELEASE}
            --
            Device: ${android.os.Build.DEVICE}
            Model: ${android.os.Build.MODEL}
            Display: ${android.os.Build.DISPLAY}
            --
            AppClassName: ${ctx.applicationInfo.className}
            Version: $version$release
            --
        """.trimIndent()
}