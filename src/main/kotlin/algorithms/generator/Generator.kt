package algorithms.generator

import com.google.gson.Gson
import dto.EdgeDTO
import dto.NetworkDTO
import dto.NodeDTO
import java.io.File
import kotlin.math.pow

class Generator(private val n: Int) {
    private val serializer = Gson()

    private fun generate(): NetworkDTO {
        val size = 4.0.pow(n.toDouble()).toInt()
        val nodes = mutableListOf<NodeDTO>()
        val edges = mutableListOf<EdgeDTO>()

        for (i in 0..size) {
            nodes.add(NodeDTO(i, "node-$i"))
            for (j in 0..size) {
                edges.add(EdgeDTO(i, j))
            }
        }
        return NetworkDTO(nodes, edges)
    }
    fun serialize(filename: String) {
        File(filename).writeText(serializer.toJson(generate()))
    }
}