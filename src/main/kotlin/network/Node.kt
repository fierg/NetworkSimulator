package network

import clocks.LamportClock
import clocks.VectorClock
import network.event.Event.logToFile
import network.event.EventType
import network.message.Message
import network.message.NodeMessage
import java.util.concurrent.atomic.AtomicInteger


/*
 * Eine Node ist eine Komponente eines verteilten Systems
 */
open class Node(
    internal val id: Int,
    private val n_nodes: Int,
    private val network: Network,
    private val useLamport: Boolean = false,
    private val useVector: Boolean = false,
    private val filepath: String,
    private val logTimeStamp: String,
)  {
    private val t_main: Thread
    private var stop = false
    private val vc = VectorClock(n_nodes)
    internal val lc = LamportClock()

    internal fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
        }
    }

    // Sample implementation is a a token ring simulation with node 0 issuing the
    // first token
    private fun run(filepath: String) {
        issueFirstToken()
        var loops = 0
        while (true) {
            loops++
            if (stop) break
            if (handleMessage(loops, filepath)) break
        }
        println("node: $id stopped.")
    }

    internal open fun handleMessage(loops: Int, filepath: String): Boolean {
        network.incrementMessageCounter()
        val rm: NodeMessage = network.receive(id) ?: return true
        val m: Message = Message.fromJson(rm.payload)
        //println("node: $id handling message ${m.toJson()}")

        val hopcount = handleHopCount(m)

        handleLamport(m)
        handleVectorClock(m)
        handleLogging(loops, hopcount, filepath)

        network.unicast(id, (id + 1) % n_nodes, m.toJson())
        println("Resend regular token from $id to ${(id + 1) % n_nodes}")
        return false
    }

    internal fun handleVectorClock(m: Message) {
        if (useVector) {
            vc.handleVectorEvent(EventType.RECEIVE, id, m, logTimeStamp)
            vc.handleVectorEvent(EventType.SEND, id, m, logTimeStamp)
        }
    }

    internal fun handleLamport(m: Message) {
        if (useLamport) {
            lc.handleLamportEvent(EventType.RECEIVE, id, m, logTimeStamp)
            lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
        }
    }

    internal fun handleHopCount(m: Message): Int {
        var hopcount: Int = m.query("hopcount")!!.toInt()
        hopcount++
        m.add("hopcount", hopcount)
        return hopcount
    }


    internal fun handleLogging(loops: Int, hopcount: Int, filepath: String) {
        if (id == 0 && loops % 1000 == 0) {
            logToFile(String.format("hopcount is %d\n", hopcount), filepath)
            if (useLamport) {
                logToFile(String.format("LC%d is %d\n", id, lc.lamportClockTimer), filepath)
            }
            if (useVector) {
                logToFile(String.format("VC%d is %s\n", id, getVectorClockString(vc.vectorClock)), filepath)
            }
        }
    }

    internal open fun issueFirstToken() {
        if (id == 0) {
            // I am node 1 and will create the token
            val m: Message = Message().add("token", "true").add("hopcount", 0)
            if (useLamport) {
                lc.handleLamportEvent(EventType.LOCAL, id, m, logTimeStamp)
                lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
            }
            if (useVector) {
                vc.handleVectorEvent(EventType.LOCAL, id, m, logTimeStamp)
                vc.handleVectorEvent(EventType.SEND, id, m, logTimeStamp)
            }
            network.unicast(id, (id + 1) % n_nodes, m.toJson())
        }
    }

    private fun getVectorClockString(vectorClock: IntArray): String {
        val sb = StringBuilder()
        for (i in vectorClock) {
            sb.append(i)
            sb.append(",")
        }
        sb.deleteCharAt(sb.length - 1)
        return sb.toString()
    }

    fun stop() {
        stop = true
        try {
            t_main.join()
        } catch (e: InterruptedException) {
            println("Stopping node error:  ${e.message}")
        }
    }

    init {
        t_main = Thread { run(filepath) }
        t_main.start()
    }
}