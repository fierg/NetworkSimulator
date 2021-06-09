package dto

import com.google.gson.annotations.SerializedName

data class NetworkDTO(@SerializedName("nodes") val nodes: List<NodeDTO>, @SerializedName("edges") val edges: List<EdgeDTO>) {
}