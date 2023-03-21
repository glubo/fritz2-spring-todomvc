package app.backend.data

import app.model.ToDo
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/todos")
class ToDoController(
    val todoService: TodoService,
) {
    @GetMapping
    fun all(): Collection<ToDo> =
        todoService.getAll()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@RequestBody toDo: ToDo): ToDo =
        todoService.add(toDo)

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    fun update(@PathVariable id: Long, @RequestBody newToDo: ToDo): ToDo =
        todoService.update(id, newToDo)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun delete(@PathVariable id: Long): Unit =
        todoService.delete(id)
}
