package com.kotlin.assets

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*

@SpringBootApplication
class AssetsApplication

fun main(args: Array<String>) {
	TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"))
	runApplication<AssetsApplication>(*args)
}
