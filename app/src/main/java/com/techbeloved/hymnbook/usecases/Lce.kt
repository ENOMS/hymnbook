package com.techbeloved.hymnbook.usecases

sealed class Lce<T>{
    data class Content<T>(val content: T): Lce<T>()
    data class Loading<T>(val loading: Boolean): Lce<T>()
    data class Error<T>(val error: String): Lce<T>()

    fun contentOrLoading(): Boolean = this is Content || this is Loading
}