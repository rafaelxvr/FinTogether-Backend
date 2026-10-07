package com.fintogether

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableJpaRepositories(basePackages = ["com.fintogether.infrastructure.persistence"])
@EnableAsync
class FinTogetherApplication

fun main(args: Array<String>) {
    runApplication<FinTogetherApplication>(*args)
}
