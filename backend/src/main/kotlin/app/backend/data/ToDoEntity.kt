package app.backend.data

import app.model.ToDo
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "Todos")
data class ToDoEntity(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        val id: Long = -1,
        val text: String = "",
        val completed: Boolean = false,
) {
    fun toToDo() = ToDo(id, text, completed)

    companion object {
        fun fromToDo(toDo: ToDo) = ToDoEntity(
            id = toDo.id,
            text = toDo.text,
            completed = toDo.completed,
        )
    }
}
