package app.frontend

import app.model.*
import dev.fritz2.core.*
import dev.fritz2.remote.http
import dev.fritz2.routing.routerOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class Filter(val text: String, val function: (List<ToDo>) -> List<ToDo>)

val filters = mapOf(
    "all" to Filter("All") { it },
    "active" to Filter("Active") { toDos -> toDos.filter { !it.completed } },
    "completed" to Filter("Completed") { toDos -> toDos.filter { it.completed } }
)

const val endpoint = "/api/todos"
val router = routerOf("all")


object ToDoListStore : RootStore<List<ToDo>>(emptyList()) {

    val todoRequest = http(endpoint)

    private val query = handle {
        val response = todoRequest.get()
        ToDo.deserializeMany(response.body())
    }

    val save = handle<ToDo> { toDos, new ->
        if (new.text.isNotBlank()) {
            val responseTodo = upsertTodoOnServer(new)
            var inList = false
            val updatedList = toDos.map {
                if (it.id == responseTodo.id) {
                    inList = true
                    responseTodo
                } else it
            }
            if (inList) updatedList else toDos + responseTodo
        } else delete(toDos, new.id)
    }

    val remove = handle<Long> { toDos, id ->
        delete(toDos, id)
    }

    val toggleAll = handle { toDos, toggle: Boolean ->
        val toUpdate = toDos.mapNotNull {
            if (it.completed != toggle) it.copy(completed = toggle) else null
        }

        val updated = (toDos + toUpdate).groupBy{ it.id }
            .filterValues { it.size > 1 }.mapValues { (id, entities) ->
                val entity = entities.last()
                upsertTodoOnServer(entity)
                entity
            }

        toDos.map { updated[it.id] ?: it }
    }

    val clearCompleted = handle { toDos ->
        toDos.partition(ToDo::completed).let { (completed, active) ->
            completed.map(ToDo::id).forEach {
                delete(listOf(), it)
            }
            active
        }
    }

    val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()

    val empty = data.map { it.isEmpty() }.distinctUntilChanged()

    val allChecked = data.map { todos -> todos.isNotEmpty() && todos.all { it.completed } }.distinctUntilChanged()
    private suspend fun delete(entities: List<ToDo>, id: Long): List<ToDo> {
        if (id != ToDo.NEW_ITEM_ID) {
            todoRequest.delete("/$id")
        }
        return entities.filterNot { it.id == id }
    }
    private suspend fun upsertTodoOnServer(new: ToDo) = if (new.isNewItem()) {
        val response = todoRequest.body(ToDo.serialize(new))
            .contentType("application/json")
            .post()

        ToDo.deserialize(response.body())
    } else {
        val response = todoRequest.body(ToDo.serialize(new))
            .contentType("application/json")
            .put("/${new.id}")

        ToDo.deserialize(response.body())
    }

    init {
        query()
    }
}

fun RenderContext.filter(text: String, route: String) {
    li {
        a {
            className(router.data.map { if (it == route) "selected" else "" })
            href("#$route")
            +text
        }
    }
}

fun RenderContext.inputHeader() {
    header {
        h1 { +"todos" }

        input("new-todo") {
            placeholder("What needs to be done?")
            autofocus(true)

            changes.values().map { domNode.value = ""; ToDo(text = it.trim()) } handledBy ToDoListStore.save
        }
    }
}

@ExperimentalCoroutinesApi
fun RenderContext.mainSection() {
    section("main") {
        input("toggle-all", id = "toggle-all") {
            type("checkbox")
            checked(ToDoListStore.allChecked)

            changes.states() handledBy ToDoListStore.toggleAll
        }
        label {
            `for`("toggle-all")
            +"Mark all as complete"
        }
        ul("todo-list") {
            ToDoListStore.data.combine(router.data) { all, route ->
                filters[route]?.function?.invoke(all) ?: all
            }.renderEach(ToDo::id) { toDo ->
                val toDoStore = ToDoListStore.mapByElement(toDo, ToDo::id)
                toDoStore.data.drop(1) handledBy ToDoListStore.save
                val textStore = toDoStore.map(ToDo.text())
                val completedStore = toDoStore.map(ToDo.completed())

                val editingStore = storeOf(false)

                li {
                    attr("data-id", toDoStore.id)
                    classMap(toDoStore.data.combine(editingStore.data) { toDo, isEditing ->
                        mapOf(
                            "completed" to toDo.completed,
                            "editing" to isEditing
                        )
                    })
                    div("view") {
                        input("toggle") {
                            type("checkbox")
                            checked(completedStore.data)

                            changes.states() handledBy completedStore.update
                        }
                        label {
                            textStore.data.renderText()

                            dblclicks.map { true } handledBy editingStore.update
                        }
                        button("destroy") {
                            clicks.map { toDo.id } handledBy ToDoListStore.remove
                        }
                    }
                    input("edit") {
                        value(textStore.data)
                        changes.values() handledBy textStore.update

                        editingStore.data handledBy { isEditing ->
                            if (isEditing) {
                                domNode.focus()
                                domNode.select()
                            }
                        }
                        merge(
                            blurs.map { false },
                            keyups.filter { shortcutOf(it) == Keys.Enter }.map { false }
                        ) handledBy editingStore.update
                    }
                }
            }
        }
    }
}

fun RenderContext.appFooter() {
    footer("footer") {
        className(ToDoListStore.empty.map { if (it) "hidden" else "" })

        span("todo-count") {
            strong {
                ToDoListStore.count.map {
                    "$it item${if (it != 1) "s" else ""} left"
                }
            }
        }

        ul("filters") {
            filters.forEach { filter(it.value.text, it.key) }
        }
        button("clear-completed") {
            +"Clear completed"

            clicks handledBy ToDoListStore.clearCompleted
        }
    }
}

@ExperimentalCoroutinesApi
fun main() {
    render("#todoapp") {
        inputHeader()
        mainSection()
        appFooter()
    }
}
