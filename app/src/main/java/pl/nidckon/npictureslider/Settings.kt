package pl.nidckon.npictureslider

class Settings private constructor(){
    var size: Int = 4
    val MIN = 3
    val MAX = 9

    var previewTime = -1

    companion object {
        private val settings = Settings()
        val const = CONST()

        fun getSettings() = settings

        class CONST{
            val PREF_NAME = "pictureslider"
            val TIME = "time"
            val SIZE = "size"
        }
    }
}