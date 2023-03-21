package app.backend.data

import app.model.ToDo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TodoService(
    val repo: ToDoRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    fun getAll(): List<ToDo> {
        logger.info("getting all ToDos")
        return repo.findAll()
            .map { it.toToDo() }
    }

    @Throws(InvalidDataException::class)
    fun add(toDo: ToDo): ToDo {
        if (toDo.text.isEmpty()) {
            throw InvalidDataException()
        }

        logger.info("save new ToDo: $toDo")
        return repo.save(
            ToDoEntity.fromToDo(toDo.copy(id = -1))
        ).toToDo()
    }

    @Throws(InvalidDataException::class, InvalidIdException::class)
    fun update(id: Long, toDo: ToDo): ToDo {
        if (repo.findById(id).isEmpty) {
            throw InvalidIdException()
        } else if (toDo.text.isEmpty()) {
            throw InvalidDataException()
        }

        logger.info("update ToDo[id=$id] to: $toDo")
        return repo.save(
            ToDoEntity.fromToDo(toDo.copy(id = id))
        ).toToDo()
    }

    @Throws(InvalidIdException::class)
    fun delete(id: Long) {
        logger.info("remove ToDo with id: $id")
        val oldToDo = repo.findById(id)
        if (oldToDo.isEmpty) {
            throw InvalidIdException()
        }
        repo.deleteById(id)
    }
}
