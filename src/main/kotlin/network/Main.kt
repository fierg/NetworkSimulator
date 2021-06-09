package network

import network.Utils.Companion.log
import java.text.SimpleDateFormat
import java.util.*

object Main {
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())
        val n_nodes = 10
        val duration = 5
        val useLamport = true
        val useVector = true
        val filepath = "log-$timeStamp.txt"
        log(String.format("Simulate %d nodes for %d seconds\n", n_nodes, duration), filepath)

        // Create network
        val network = Network(n_nodes, useLamport, useVector, filepath)

        // Create all nodes and start them
        val nodes = HashMap<Int, Node>()
        for (n_id in 0 until n_nodes) nodes[n_id] =
            Node(n_id, n_nodes, network, useLamport, useVector, filepath, timeStamp)

        // Wait for the required duration
        Thread.sleep((duration * 1000).toLong())

        log("Stopping network...", filepath)

        // Stop network - release nodes waiting in receive ...
        network.stop()

        // Tell all nodes to stop and wait for the threads to terminate
        for (n in nodes.values) {
            n.stop()
        }
        log("All nodes stopped. Terminating.", filepath)
        log("Total messages sent: ${network.getMessageCounter()}")
    }
}