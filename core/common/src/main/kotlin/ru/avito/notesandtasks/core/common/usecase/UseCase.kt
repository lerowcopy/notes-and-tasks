package ru.avito.notesandtasks.core.common.usecase

interface UseCase<in P, out R> {
    suspend operator fun invoke(parameters: P): R
}
