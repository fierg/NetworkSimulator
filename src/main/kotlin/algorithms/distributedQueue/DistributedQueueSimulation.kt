package algorithms.distributedQueue

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import network.Network
import network.Node
import network.Utils
import network.message.Message
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class DistributedQueueSimulation {
    @OptIn(DelicateCoroutinesApi::class)
    suspend fun simulate() {

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
            DistributedQueueNode(n_id, n_nodes, network, filepath, timeStamp, linkedSetOf(0,1,2).toList() )


        val job = GlobalScope.launch {
            repeat(1000) { i ->
                val m =  Message().add("QUEUE", "ADD-${Random.nextInt(0, n_nodes)}")
                network.broadcast(-1, m.toJson())
                delay(Random.nextInt(0, 50).toLong())
            }
        }


        // Wait for the required duration
        Thread.sleep((duration * 1000).toLong())

        job.cancel() // cancels the job
        job.join() // waits for job's completion

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
suspend fun main(){
    val vc = DistributedQueueSimulation()
    vc.simulate()
}