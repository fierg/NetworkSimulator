package algorithms.distributedQueue

import network.Network
import network.Node
import network.Utils
import network.event.EventType
import network.message.Message
import network.message.NodeMessage
import kotlin.random.Random

class DistributedQueueNode(
    id: Int,
    private val n_nodes: Int,
    private val network: Network,
    private val filepath: String,
    private val logTimeStamp: String,
    private var queue: List<Int>

) : Node(id, n_nodes, network, useLamport = true, false, filepath, logTimeStamp) {

    override fun issueFirstToken() {
        if (id == 0) {
            // I am node 1 and will create the token
            val m: Message = Message().add("hopcount", 0)
            lc.handleLamportEvent(EventType.LOCAL, id, m, logTimeStamp)
            lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
            m.add("QUEUE", "REL-0")
            network.broadcast(id, m.toJson())
        }
    }


    override fun handleMessage(loops: Int, filepath: String): Boolean {
        val rm: NodeMessage = network.receive(id) ?: return true
        val m: Message = Message.fromJson(rm.payload)

        println("node: $id handling message ${m.toJson()}")
        sleep(20)

        if (handleQueueUpdate(m, rm))
            return false

        handleLamport(m)
        handleLogging(loops, 0, filepath)
        Utils.criticalSection("node: $id")

        //network.broadcast(id, Message().add("QUEUE", "ADD-${Random.nextInt(0, n_nodes)}").toJson())

        tryQueueUpdate()

        return false
    }

    private fun tryQueueUpdate() {
        val m = Message().add("QUEUE", "REL-$id")
        lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
        network.broadcast(id, m.toJson())
    }

    private fun handleQueueUpdate(m: Message, rm: NodeMessage): Boolean {
        return when {
            m.query("QUEUE") == null -> return false
            m.query("QUEUE")!!.matches(Regex("REL-(\\d+)")) -> {
                if (queue.isEmpty()){
                    throw IllegalArgumentException("Empty queue in node $id received release")
                }
                val value = Regex("REL-(\\d+)").find(m.query("QUEUE")!!)?.groupValues?.get(1)!!.toInt()
                if (queue.first() == value)
                    queue = queue.drop(1)
                else
                    throw IllegalArgumentException("Release doesnt match first element. node $id tried to release $value, first is ${queue.first()}")

                //break if id is first in queue
                queue.first() != id
            }
            m.query("QUEUE")!!.matches(Regex("ADD-(\\d+)")) -> {
                queue = queue.plusElement(Regex("ADD-(\\d+)").find(m.query("QUEUE")!!)?.groupValues?.get(1)!!.toInt())
                true
            }
            else -> {
                System.err.println("Queue Error for message: ${m.query("Queue")}")
                false
            }
        }
    }
}