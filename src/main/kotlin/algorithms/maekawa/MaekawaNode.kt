package algorithms.maekawa

import network.Network
import network.Node
import network.Utils
import network.message.Message
import network.message.NodeMessage
import kotlin.math.sqrt
import kotlin.random.Random

class MaekawaNode(
    id: Int,
    private val n_nodes: Int,
    private val network: Network,
    private val useLamport: Boolean = false,
    private val useVector: Boolean = false,
    private val filepath: String,
    private val logTimeStamp: String,
) : Node(id, n_nodes, network, useLamport, useVector, filepath, logTimeStamp) {

    private var maekawaBlocked = -1
    private var acksReceived = 0

    override fun handleMessage(loops: Int, filepath: String): Boolean {
        network.incrementMessageCounter()
        val rm: NodeMessage = network.receive(id) ?: return true
        val m: Message = Message.fromJson(rm.payload)
        println("node: $id handling message ${m.toJson()}")

        if (handleMaekawaMessage(m, rm))
            return false

        val hopcount = handleHopCount(m)

        handleLamport(m)
        handleVectorClock(m)
        handleLogging(loops, hopcount, filepath)

        if (tryMaekawaRequest(m)) return false

        network.unicast(id, (id + 1) % n_nodes, m.toJson())
        println("Resend regular token from $id to ${(id + 1) % n_nodes}")
        return false
    }


    private fun tryMaekawaRequest(originalMessage: Message): Boolean {
        sleep(20)
        val nodesRequired = getRequiredNodes()
        for (i in 0..2) {
            try {
                println("node: $id tries to acquire maekawa lock... (try $i)")
                network.multicast(id, nodesRequired, Message().add("Maekawa", "REQ").toJson())

                waitForMaekawaLock()

                Utils.criticalSection("node: $id")

                println("Resend regular token from $id to ${(id + 1) % n_nodes}")
                network.unicast(id, (id + 1) % n_nodes, originalMessage.toJson())

                releaseMaekawaLock(nodesRequired)

                break
            } catch (e: MaekawaException) {
                System.err.println("Maekawa Error $i for node: $id")
                releaseMaekawaLock(nodesRequired)
                //sleep random to increase resilience against dead locks
                sleep(Random.nextInt(20, 100).toLong())
                return false
            }
        }
        return true
    }

    private fun waitForMaekawaLock(): Boolean {
        var waiting = true
        while (waiting) {
            val rm: NodeMessage = network.receive(id) ?: return true
            val m: Message = Message.fromJson(rm.payload)
            handleMaekawaMessage(m, rm)
            if (acksReceived == 2 * sqrt(n_nodes.toDouble()).toInt() - 2) {
                println("### node: $id acquired maekawa lock. ###")
                sleep(20)
                waiting = false
            }
        }
        return false
    }

    private fun releaseMaekawaLock(nodesRequired: List<Int>) {
        println("node: $id tries to release maekawa lock.")
        network.multicast(id, nodesRequired, Message().add("Maekawa", "REL").toJson())
        println("node: $id released maekawa lock.")
    }

    private fun getRequiredNodes(): List<Int> {
        val nodesRequired = mutableListOf<Int>()
        val maekawaSize = sqrt(n_nodes.toDouble()).toInt()
        for (j in 0 until maekawaSize) {
            //nodes in same row
            nodesRequired.add(j * maekawaSize + (id % maekawaSize))
            //nodes in same column
            nodesRequired.add((id / maekawaSize) * maekawaSize + j)
        }
        return nodesRequired.filter { it != id }.toList()
    }

    private fun handleMaekawaMessage(m: Message, rm: NodeMessage): Boolean {
        return when (m.query("Maekawa")) {
            "REQ" -> {
                println("node: $id received REQ from ${rm.senderId}")
                if (maekawaBlocked == -1 || maekawaBlocked == rm.senderId) {
                    network.unicast(id, rm.senderId, Message().add("Maekawa", "ACK").toJson())
                    maekawaBlocked = rm.senderId
                } else
                    network.unicast(id, rm.senderId, Message().add("Maekawa", "ERR").toJson())
                true
            }
            "REL" -> {
                println("node: $id received REL from ${rm.senderId}")
                if (maekawaBlocked == rm.senderId)
                    maekawaBlocked = -1
                true
            }
            "ACK" -> {
                println("node: $id received ACK from ${rm.senderId}")
                acksReceived++
                true
            }
            "ERR" -> {
                println("node: $id received ERR from ${rm.senderId}")
                acksReceived = 0
                throw MaekawaException()
            }
            null -> {
                return false
            }
            else -> {
                System.err.println("Maekawa Error for message: ${m.query("Maekawa")}")
                false
            }
        }
    }

}