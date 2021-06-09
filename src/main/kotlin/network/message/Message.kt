package network.message

import java.lang.reflect.Type
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Message {
    private val content: HashMap<String, String>

    constructor() {
        content = HashMap()
    }

    private constructor(content: HashMap<String, String>) {
        this.content = content
    }

    fun add(key: String, value: String): Message {
        content[key] = value
        return this
    }

    fun add(key: String, value: Int): Message {
        return add(key, value.toString())
    }

    fun query(key: String): String? {
        return content[key]
    }

    val map: Map<String, String>
        get() = content

    fun toJson(): String {
        return serialize(content)
    }

    companion object {
        private val serializer: Gson = Gson()
        fun fromJson(s: String?): Message {
            val contentType: Type = object : TypeToken<HashMap<String?, String?>?>() {}.getType()
            return Message(serializer.fromJson(s, contentType))
        }

        @Synchronized
        private fun serialize(content: Map<String, String>): String {
            return serializer.toJson(content) // Not sure about thread safety of Gson
        }
    }
}