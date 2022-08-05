package io.github.scidev5.util

private class RegexSetEntry(
    val regex: Regex,
    var nAllowedMatches: Int,
    val callback: (MatchResult) -> Unit
)

class RegexMany {
    private val regexSetEntries = MutableList(0) { RegexSetEntry(Regex(""),0){} }
    fun add(pattern: String, nAllowedMatches: Int, callback: (MatchResult) -> Unit) {
        regexSetEntries.add(RegexSetEntry(
            Regex(pattern,RegexOption.IGNORE_CASE),
            nAllowedMatches,
            callback
        ))
    }
    operator fun invoke(string: String):Boolean {
        var found:RegexSetEntry? = null
        for (entry in regexSetEntries) {
            entry.callback(
                entry.regex.matchEntire(string) ?: continue
            )
            found = entry
            break
        }
        if (found != null) {
            if (found.nAllowedMatches >= 0)
                --found.nAllowedMatches
            if (found.nAllowedMatches == 0)
                regexSetEntries.remove(found)
        }
        return found != null
    }

}