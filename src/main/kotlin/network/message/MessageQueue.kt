package network.message

import java.util.*
import java.util.concurrent.Semaphore

class MessageQueue {
    private val queue: LinkedList<NodeMessage> = LinkedList()
    private val sema: Semaphore = Semaphore(0)
    private var stop: Boolean = false

    fun put(r: NodeMessage) {
        synchronized(queue) {
            queue.addLast(r)
            sema.release()
        }
    }

    fun waitForMessage(): NodeMessage? {
        while (true) {
            if (stop) return null
            try {
                sema.acquire()
                synchronized(queue) { if (!queue.isEmpty()) return queue.removeFirst() }
            } catch (e: InterruptedException) {
                println(e.message)
            }
        }
    }

    fun stop() {
        stop = true
    }
}