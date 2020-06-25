package dev.fritz2.frontend

import dev.fritz2.binding.*
import dev.fritz2.dom.append
import dev.fritz2.dom.html.HtmlElements
import dev.fritz2.dom.html.Keys
import dev.fritz2.dom.html.render
import dev.fritz2.dom.key
import dev.fritz2.dom.states
import dev.fritz2.dom.values
import dev.fritz2.lenses.Lens
import dev.fritz2.lenses.buildLens
import dev.fritz2.remote.body
import dev.fritz2.remote.remote
import dev.fritz2.routing.router
import dev.fritz2.frontend.model.ToDo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.js.Json

data class Filter(val text: String, val function: (List<ToDo>) -> List<ToDo>)

val filters = mapOf(
    "/" to Filter("All") { it },
    "/active" to Filter("Active") { toDos -> toDos.filter { !it.completed } },
    "/completed" to Filter("Completed") { toDos -> toDos.filter { it.completed } }
)

object L {
    val completed: Lens<ToDo, Boolean> = buildLens("completed", { it.completed },{ p, v -> p.copy(completed = v)})

    val editing: Lens<ToDo, Boolean> = buildLens("editing", { it.editing },{ p, v -> p.copy(editing = v)})

    val text: Lens<ToDo, String> = buildLens("text", { it.text },{ p, v -> p.copy(text = v)})
}

@ExperimentalCoroutinesApi
@FlowPreview
fun main() {
    val router = router("/")

    val toDos = object : RootStore<List<ToDo>>(emptyList()) {

//        val rest = remote("/todo")
//        val json = Json(JsonConfiguration.Stable)
//
//        val load = apply<ToDo> {
//            rest.get().body()
//        }

        val add = handle<String> { toDos, text ->
            if (text.isNotEmpty()) toDos + ToDo(text)
            else toDos
        }

        val remove = handle<String> { toDos, id ->
            toDos.filterNot { it.id == id }
        }

        val toggleAll = handle<Boolean> { toDos, toggle ->
            toDos.map { it.copy(completed = toggle) }
        }

        val clearCompleted = handle { toDos ->
            toDos.filterNot { it.completed }
        }

        val count = data.map { todos -> todos.count { !it.completed } }.distinctUntilChanged()
        val allChecked = data.map { todos -> todos.isNotEmpty() && todos.all { it.completed } }.distinctUntilChanged()
    }

    val inputHeader = render {
        header {
            h1 { text("todos") }
            input("new-todo") {
                placeholder = const("What needs to be done?")
                autofocus = const(true)

                changes.values().onEach { domNode.value = "" } handledBy toDos.add
            }
        }
    }

    val mainSection = render {
        section("main") {
            input("toggle-all", id = "toggle-all") {
                type = const("checkbox")
                checked = toDos.allChecked

                changes.states() handledBy toDos.toggleAll
            }
            label(`for` = "toggle-all") {
                text("Mark all as complete")
            }
            ul("todo-list") {
                toDos.data.flatMapLatest { all ->
                    router.routes.map { route ->
                        filters[route]?.function?.invoke(all) ?: all
                    }
                }.each().map { toDo ->
                    val toDoStore = toDos.sub(toDo)
                    val textStore = toDoStore.sub(L.text)
                    val completedStore = toDoStore.sub(L.completed)
                    val editingStore = toDoStore.sub(L.editing)

                    render {
                        li {
                            attr("data-id", toDoStore.id)
                            //TODO: better flatmap over editing and completed
                            classMap = toDoStore.data.map { toDo: ToDo ->
                                mapOf(
                                    "completed" to toDo.completed,
                                    "editing" to toDo.editing
                                )
                            }
                            div("view") {
                                input("toggle") {
                                    type = const("checkbox")
                                    checked = completedStore.data

                                    changes.states() handledBy completedStore.update
                                }
                                label {
                                    textStore.data.bind()

                                    dblclicks.map { true } handledBy editingStore.update
                                }
                                button("destroy") {
                                    clicks.events.map { toDo.id } handledBy toDos.remove //flatMapLatest { toDoStore.data }
                                }
                            }
                            input("edit") {
                                value = textStore.data
                                changes.values() handledBy textStore.update

                                editingStore.data.map { isEditing: Boolean ->
                                    if (isEditing) domNode.apply {
                                        focus()
                                        select()
                                    }
                                    isEditing.toString()
                                }.watch()
                                merge(
                                    blurs.map { false },
                                    keyups.key().filter { it.isKey(Keys.Enter) }.map { false }
                                ) handledBy editingStore.update
                            }
                        }
                    }
                }.bind()
            }
        }
    }

    fun HtmlElements.filter(text: String, route: String) {
        li {
            a {
                className = router.routes.map { if (it == route) "selected" else "" }
                href = const("#$route")
                text(text)
            }
        }
    }

    val appFooter = render {
        footer("footer") {
            span("todo-count") {
                strong {
                    toDos.count.map {
                        "$it item${if (it != 1) "s" else ""} left"
                    }.bind()
                }
            }

            ul("filters") {
                filters.forEach { filter(it.value.text, it.key) }
            }
            button("clear-completed") {
                text("Clear completed")

                clicks handledBy toDos.clearCompleted
            }
        }
    }

    append("todoapp", inputHeader, mainSection, appFooter)
}