package algorithms.maekawa

import network.Network
import network.Utils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class Maekawa {

    fun simulate() {

        val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())
        val nNodes = 10
        val duration = 4
        val filepath = "log-$timeStamp.txt"
        Utils.log(
            String.format("Simulate %d nodes for %d seconds\n", nNodes.toDouble().pow(2).toInt(), duration),
            filepath
        )

        // Create network
        val network = Network(nNodes.toDouble().pow(2).toInt(), filepath = filepath)

        // Create all nodes and start them
        val nodes = (0 until nNodes).map { i ->
            (0 until nNodes).map { j ->
                MaekawaNode(
                    (i * nNodes) + j,
                    nNodes.toDouble().pow(2).toInt(),
                    network,
                    filepath = filepath,
                    logTimeStamp = timeStamp,
                )
            }.toTypedArray()
        }.toTypedArray()


        // Wait for the required duration
        Thread.sleep((duration * 1000).toLong())

        Utils.log("Stopping network...", filepath)

        // Stop network - release nodes waiting in receive ...
        network.stop()

        Utils.log("Total messages sent: ${network.getMessageCounter()}")

        // Tell all nodes to stop and wait for the threads to terminate
        for (nodesArray in nodes)
            for (node in nodesArray)
                node.stop()

        Utils.log("All nodes stopped. Terminating.", filepath)
    }
}

fun main() {
    val m = Maekawa()
    m.simulate()
}

