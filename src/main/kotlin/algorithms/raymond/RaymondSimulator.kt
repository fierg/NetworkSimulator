package algorithms.raymond

import network.Network
import network.Node
import network.Utils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

class RaymondSimulator {

    fun simulate() {

        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())
        val n_nodes = 10
        val duration = 5
        val filepath = "log-$timeStamp.txt"
        Utils.log(String.format("Simulate %d nodes for %d seconds\n", n_nodes, duration), filepath)

        // Create network
        val network = Network(n_nodes, useLamport = true, useVector = false, filepath)

        // Create all nodes and start them
        val nodes = HashMap<Int, Node>()
        for (n_id in 0 until n_nodes) nodes[n_id] =
            RaymondNode(
                n_id,
                n_nodes,
                network,
                filepath,
                timeStamp,
                floor(((n_id - 1) / 2).toDouble()).toInt(),
                n_id == 0
            )

        // Wait for the required duration
        Thread.sleep((duration * 1000).toLong())

        Utils.log("Stopping network...", filepath)

        // Stop network - release nodes waiting in receive ...
        network.stop()

        // Tell all nodes to stop and wait for the threads to terminate
        for (n in nodes.values) {
            n.stop()
        }

        Utils.log("Total messages sent: ${network.getMessageCounter()}")
        Utils.log("All nodes stopped. Terminating.", filepath)
    }
}

fun main() {
    val vc = RaymondSimulator()
    vc.simulate()
}
