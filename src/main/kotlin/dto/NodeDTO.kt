package dto

import com.google.gson.annotations.SerializedName

data class NodeDTO(@SerializedName("id") val id: Int, @SerializedName("name") val name: String) {
}