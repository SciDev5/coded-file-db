package io.github.scidev5


val HEX_CHARS = "0123456789abcdef".toCharArray();
fun hexEncode(buff: ByteArray):String {
    var out = ""
    for (byte in buff) {
        out += HEX_CHARS[byte.toInt() and 0xf0 shr 4]
        out += HEX_CHARS[byte.toInt() and 0x0f]
    }
    return out
}
