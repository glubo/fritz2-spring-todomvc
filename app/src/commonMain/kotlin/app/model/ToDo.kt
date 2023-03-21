package app.model

import dev.fritz2.core.Lenses
import dev.fritz2.validation.Validation
import dev.fritz2.validation.ValidationMessage
import dev.fritz2.validation.validation
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ToDoMessage(val id: String, val text: String): ValidationMessage {
    override val isError: Boolean
        get() = true
    override val path: String
        get() = TODO("Not yet implemented")
}


@Serializable
@Lenses
data class ToDo(
    val id: Long = NEW_ITEM_ID,
    val text: String,
    val completed: Boolean = false
){
    fun isNewItem() = id == NEW_ITEM_ID
    companion object{
        const val NEW_ITEM_ID = -1L

        private const val maxTextLength = 50
        val validation: Validation<ToDo, Unit, ToDoMessage> = validation<ToDo, ToDoMessage>{ inspector ->
            val textInspector = inspector.map(ToDo.text())
            if (textInspector.data.trim().length < 3) {
                add(ToDoMessage(textInspector.path, "Text length must be at least 3 characters"))
            }
            if (textInspector.data.trim().length > maxTextLength) {
                add(ToDoMessage(textInspector.path, "Text length is to long (max $maxTextLength chars)."))
            }
        }

        fun deserialize(source: String) = Json.decodeFromString<ToDo>(source)
        fun deserializeMany(source: String) = Json.decodeFromString<List<ToDo>>(source)

        fun serialize(item: ToDo) = Json.encodeToString(item)
    }
}
