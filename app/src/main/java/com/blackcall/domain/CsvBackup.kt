package com.blackcall.domain

data class BackupEntry(val kind: String, val value: String, val label: String)

data class ParsedBackup(val entries: List<BackupEntry>, val skipped: Int)

/** Tiny CSV format: `type,value,label` where type is BLOCK, ALLOW or PREFIX. */
object CsvBackup {
    const val HEADER = "type,value,label"
    private val kinds = setOf("BLOCK", "ALLOW", "PREFIX")

    fun export(entries: List<BackupEntry>): String =
        (listOf(HEADER) + entries.map { "${it.kind},${escape(it.value)},${escape(it.label)}" })
            .joinToString("\n")

    fun parse(text: String): ParsedBackup {
        val out = mutableListOf<BackupEntry>()
        var skipped = 0
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            if (line.equals(HEADER, ignoreCase = true)) return@forEach
            val f = splitLine(line)
            val kind = f.getOrNull(0)?.trim()?.uppercase()
            val value = f.getOrNull(1)?.trim().orEmpty()
            val valid = kind in kinds && when (kind) {
                "PREFIX" -> PhoneNumbers.normalizePrefix(value) != null
                else -> PhoneNumbers.isValidNumber(value)
            }
            if (valid) out += BackupEntry(kind!!, value, f.getOrNull(2)?.trim().orEmpty()) else skipped++
        }
        return ParsedBackup(out, skipped)
    }

    private fun escape(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun splitLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                quoted && c == '"' && line.getOrNull(i + 1) == '"' -> { sb.append('"'); i++ }
                c == '"' -> quoted = !quoted
                c == ',' && !quoted -> { fields += sb.toString(); sb.clear() }
                else -> sb.append(c)
            }
            i++
        }
        fields += sb.toString()
        return fields
    }
}
