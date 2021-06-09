package algorithms.raymond

import network.Network
import network.Node
import network.event.EventType
import network.message.Message
import network.message.NodeMessage
import kotlin.random.Random

class RaymondNode(
    id: Int,
    private val n_nodes: Int,
    private val network: Network,
    private val filepath: String,
    private val logTimeStamp: String,
    private var tokenDirection: Int?,
    var hasToken: Boolean = false
) : Node(id, n_nodes, network, useLamport = true, useVector = false, filepath, logTimeStamp) {

    var lastTokenRequestDirection: Int? = null
    var wantToken = false

    override fun issueFirstToken() {
        if (id == 0) {
            // I am node 1 and will create the token
            val m: Message = Message().add("hopcount", 0)
            lc.handleLamportEvent(EventType.LOCAL, id, m, logTimeStamp)
            lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
            m.add("RAYMOND", "next-request")
            network.unicast(id, Random.nextInt(0, n_nodes), m.toJson())
        }
    }

    override fun handleMessage(loops: Int, filepath: String): Boolean {
        network.incrementMessageCounter()
        val rm: NodeMessage = network.receive(id) ?: return true
        val m: Message = Message.fromJson(rm.payload)
        println("node: $id handling message ${m.toJson()}")

        handleTokenRequest(m, rm)
        return false
    }

    private fun handleTokenRequest(m: Message, rm: NodeMessage) {
        when (m.query("RAYMOND")) {
            "token" -> {
                lc.handleLamportEvent(EventType.RECEIVE, id, m, logTimeStamp)
                lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)

                if (hasToken) grantToken(m, rm)
                else askNeighbourForToken(m, rm)
                return
            }
            "granted" -> {
                hasToken = true
                tokenDirection = null

                if (wantToken) createNextRequest(m)
                else passToken(m)

                return
            }
            "next-request" -> {
                handleNextRequest(m)
                return
            }
            else -> System.err.println("Raymond Error for message: ${m.query("RAYMOND")}")

        }
    }

    private fun handleNextRequest(m: Message) {
        wantToken = true
        m.add("hopcount", handleHopCount(m))
        lc.handleLamportEvent(EventType.LOCAL, id, m, logTimeStamp)
        lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
        m.add("RAYMOND", "token")
        network.unicast(
            id,
            tokenDirection ?: throw IllegalArgumentException("Token direction for node $id is null"),
            m.toJson()
        )
    }

    private fun passToken(m: Message) {
        m.add("hopcount", handleHopCount(m))
        lc.handleLamportEvent(EventType.LOCAL, id, m, logTimeStamp)
        lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
        m.add("RAYMOND", "granted")
        network.unicast(id, lastTokenRequestDirection!!, m.toJson())
        lastTokenRequestDirection = null
        hasToken = false
        tokenDirection = lastTokenRequestDirection
    }

    private fun askNeighbourForToken(m: Message, rm: NodeMessage) {
        m.add("RAYMOND", "token")
        handleHopCount(m)
        network.unicast(
            id,
            tokenDirection ?: throw IllegalArgumentException("Token direction for node $id is null"),
            m.toJson()
        )
        lastTokenRequestDirection = rm.senderId
    }

    private fun grantToken(m: Message, rm: NodeMessage) {
        m.add("RAYMOND", "granted")
        handleHopCount(m)
        tokenDirection = rm.senderId
        hasToken = false
        network.unicast(id, rm.senderId, m.toJson())
    }

    private fun createNextRequest(m: Message) {
        m.add("hopcount", handleHopCount(m))
        lc.handleLamportEvent(EventType.LOCAL, id, m, logTimeStamp)
        lc.handleLamportEvent(EventType.SEND, id, m, logTimeStamp)
        m.add("RAYMOND", "next-request")
        network.unicast(id, Random.nextIntExclusive(0, n_nodes, id), m.toJson())
        wantToken = false
    }
}

private fun Random.nextIntExclusive(i: Int, nNodes: Int, id: Int): Int {
    for (i in 0..10) {
        val random = Random.nextInt(0, nNodes)
        if (random == id) break
        return random
    }
    return -1
}
