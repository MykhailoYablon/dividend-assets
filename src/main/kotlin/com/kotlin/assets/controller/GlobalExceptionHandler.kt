package com.kotlin.assets.controller

import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidationError(e: IllegalArgumentException, model: Model): String {
        // Expose only known safe messages; do not leak internal details
        val safeMessage = e.message?.takeIf { it.length < 200 && !it.contains("at com.") }
            ?: "Invalid request. Please check your input and try again."
        model.addAttribute("error", safeMessage)
        return "error"
    }
}