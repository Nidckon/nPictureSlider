package pl.nidckon.npictureslider

class Settings private constructor(){

    var size: Int = 4
    val MIN = 3
    val MAX = 9

    companion object {
        private val settings = Settings()

        fun getSettings() = settings
    }
}