package network.event

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object Event {
    fun logToFile(log: String, filepath: String?) {
        if (filepath != null) {
            try {
                Files.write(
                    Paths.get(filepath),
                    log.toByteArray(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}