package app.backend.data

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST, reason = "data is not valid")
class InvalidDataException : RuntimeException()

@ResponseStatus(HttpStatus.BAD_REQUEST, reason = "id is not valid")
class InvalidIdException : RuntimeException()
