package net.civmc.kira.rabbit

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val rabbitModule = module(createdAtStart = true) {
    singleOf(::RabbitService)
}
