package io.github.scidev5.util

/**
 * Split the collection into two lists depending on if filter returns true.
 * First item in pair is true in filter, second is false
 */
inline fun <T> fillet(array: Collection<T>, filter:(T)->Boolean):Pair<List<T>,List<T>> {
    val positive = array.filter(filter)
    return Pair(
        positive,
        array.filter { it !in positive }
    )
}