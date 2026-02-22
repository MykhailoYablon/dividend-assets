package com.kotlin.assets.controller

import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidationError(e: IllegalArgumentException, model: Model): String {
        model.addAttribute("error", e.message)
        return "error"
    }
}