package clocks

import network.event.Event.logToFile
import network.event.EventType
import network.message.Message

class VectorClock(n_nodes: Int) {

    val vectorClock = IntArray(n_nodes)

    fun handleVectorEvent(
        type: EventType,
        node: Int,
        m: Message,
        logTimeStamp: String
    ): IntArray {
        return when (type) {
            EventType.LOCAL -> handleLocalVectorEvent(node, m, logTimeStamp)
            EventType.SEND -> handleSendVectorEvent(node, m, logTimeStamp)
            EventType.RECEIVE -> handleReceiveVectorEvent(node, m, logTimeStamp)
            else -> throw IllegalArgumentException("UNKNOWN LAMPORT EVENT")
        }
    }

    @Synchronized
    fun handleReceiveVectorEvent(node: Int, m: Message, logTimeStamp: String): IntArray {
        logToFile(
            "Receive Event | VC" + node + " | " + getVectorClockString(vectorClock),
            "log-$logTimeStamp-VECTOR.txt"
        )
        val receivedVectorClock: IntArray =
            m.query("VC")!!.split(";")[1].split(",").map { it -> it.toInt() }.toIntArray()
        vectorClock[node] += 1
        for (i in vectorClock.indices) {
            vectorClock[i] = vectorClock[i].coerceAtLeast(receivedVectorClock[i])
        }
        logToFile(
            " -> VC$node | ${getVectorClockString(vectorClock)}",
            "log-$logTimeStamp-VECTOR.txt"
        )
        return vectorClock
    }

    @Synchronized
    fun handleSendVectorEvent(node: Int, m: Message, logTimeStamp: String): IntArray {
        logToFile(
            "Send Event | VC" + node + " | " + getVectorClockString(vectorClock),
            "log-$logTimeStamp-VECTOR.txt"
        )
        vectorClock[node] += 1
        logToFile(
            " -> VC$node | ${getVectorClockString(vectorClock)}",
            "log-$logTimeStamp-VECTOR.txt"
        )
        m.add("VC", node.toString() + ";" + getVectorClockString(vectorClock))
        return vectorClock
    }

    @Synchronized
    fun handleLocalVectorEvent(node: Int, m: Message, logTimeStamp: String): IntArray {
        logToFile(
            "Local Event | VC" + node + " | " + getVectorClockString(vectorClock),
            "log-$logTimeStamp-VECTOR.txt"
        )
        vectorClock[node] += 1
        logToFile(
            " -> VC$node | ${getVectorClockString(vectorClock)}",
            "log-$logTimeStamp-VECTOR.txt"
        )
        return vectorClock
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

}