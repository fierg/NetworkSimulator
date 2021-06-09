package clocks

import network.event.Event
import network.event.EventType
import network.message.Message

class LamportClock {
     var lamportClockTimer = 0

    fun handleLamportEvent(type: EventType?, node: Int, m: Message, logTimeStamp: String): Int {
        return when (type) {
            EventType.LOCAL -> handleLocalLamportEvent(node, m, logTimeStamp)
            EventType.SEND -> handleSendLamportEvent(node, m, logTimeStamp)
            EventType.RECEIVE -> handleReceiveLamportEvent(node, m, logTimeStamp)
            else -> throw IllegalArgumentException("UNKNOWN LAMPORT EVENT")
        }
    }


    @Synchronized
    fun handleSendLamportEvent(node: Int, m: Message, logTimeStamp: String): Int {
        Event.logToFile(
            "Send Event | LC$node;$lamportClockTimer -> LC$node;${lamportClockTimer + 1}",
            "log-$logTimeStamp-LAMPORT.txt"
        )
        m.add("LC", node.toString() + ";" + ++lamportClockTimer)
        return lamportClockTimer
    }

    @Synchronized
    fun handleLocalLamportEvent(node: Int, m: Message, logTimeStamp: String): Int {
        Event.logToFile(
            "Local Event | LC$node;$lamportClockTimer -> LC$node;${lamportClockTimer + 1}",
            "log-$logTimeStamp-LAMPORT.txt"
        )
        return ++lamportClockTimer
    }

    @Synchronized
    fun handleReceiveLamportEvent(node: Int, m: Message, logTimeStamp: String): Int {
        val receivedLamportClock = m.query("LC")!!.split(";")[1].toInt()
        val max = Math.max(receivedLamportClock, lamportClockTimer) + 1
        Event.logToFile("Receive Event | LC$node;$lamportClockTimer -> LC$node;$max\n", "log-$logTimeStamp-LAMPORT.txt")
        return max
    }

}