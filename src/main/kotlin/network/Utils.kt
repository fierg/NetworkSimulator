package network

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.Semaphore

class Utils {
    companion object {
        private val sema: Semaphore = Semaphore(0)

        fun log(log: String, filepath: String? = null) {
            println(log)
            if (!filepath.isNullOrEmpty()){
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

        fun criticalSection(payload: String){
            //sema.acquire()
            Thread.sleep(200)
            try {
                Files.write(
                    Paths.get("criticalSection.txt"),
                    payload.toByteArray(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
                )
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                //sema.release()
            }
        }
    }
}