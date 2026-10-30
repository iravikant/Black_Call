package com.blackcall.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvBackupTest {

    @Test fun roundTripPreservesEntriesAndAwkwardLabels() {
        val entries = listOf(
            BackupEntry("BLOCK", "9876543210", "Loan, \"fast\" cash"),
            BackupEntry("ALLOW", "+91 98111 11111", ""),
            BackupEntry("PREFIX", "+92", "Pakistan")
        )
        val parsed = CsvBackup.parse(CsvBackup.export(entries))
        assertEquals(entries, parsed.entries)
        assertEquals(0, parsed.skipped)
    }

    @Test fun invalidRowsAreSkippedNotFatal() {
        val text = "type,value,label\nBLOCK,12,too short\nNOPE,9876543210,x\nPREFIX,abc,x\nBLOCK,9876543210,ok"
        val parsed = CsvBackup.parse(text)
        assertEquals(1, parsed.entries.size)
        assertEquals(3, parsed.skipped)
    }

    @Test fun headerAndBlankLinesAreIgnored() {
        assertEquals(0, CsvBackup.parse("type,value,label\n\n").skipped)
    }
}
