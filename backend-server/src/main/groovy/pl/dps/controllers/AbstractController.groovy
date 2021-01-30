package pl.dps.controllers

import javassist.NotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler

abstract class AbstractController {
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity dataIntegrityViolationException(DataIntegrityViolationException e) {
        e.printStackTrace()
        return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity illegalArgumentExceptionHandler(IllegalArgumentException e) {
        e.printStackTrace()
        return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity notFoundExceptionHandler(NotFoundException e) {
        e.printStackTrace()
        return new ResponseEntity(e.getMessage(), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity exceptionHandler(Exception e) {
        e.printStackTrace()
        return new ResponseEntity(e.getMessage() + " " + e.class, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity notValidArgumentExceptionHandler(MethodArgumentNotValidException e) {
        e.printStackTrace()
        return new ResponseEntity("At least one not valid argument", HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity HttpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        e.printStackTrace()
        return new ResponseEntity("At least one not valid argument", HttpStatus.BAD_REQUEST)
    }
}
