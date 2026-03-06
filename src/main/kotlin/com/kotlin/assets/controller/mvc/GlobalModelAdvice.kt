package com.kotlin.assets.controller.mvc

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

@ControllerAdvice
class GlobalModelAdvice(private val request: HttpServletRequest) {

    @ModelAttribute("requestURI")
    fun requestURI(): String = request.requestURI
}
