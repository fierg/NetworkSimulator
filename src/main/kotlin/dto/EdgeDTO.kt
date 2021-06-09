package dto

import com.google.gson.annotations.SerializedName

data class EdgeDTO(@SerializedName("from") val from:Int, @SerializedName("to") val to:Int, @SerializedName("weight") val weight: Double = 1.0) {
}