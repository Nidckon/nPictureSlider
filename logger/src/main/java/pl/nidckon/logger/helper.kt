package pl.nidckon.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger {
    return LoggerFactory.getLogger(T::class.java)
}

fun Logger(){

}


class klasa {
       val LOG = logger()

    fun add(a:Int, b:Int): Int {
        LOG.info()
    }
}