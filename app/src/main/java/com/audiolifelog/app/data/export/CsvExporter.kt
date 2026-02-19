package com.audiolifelog.app.data.export

import com.audiolifelog.app.data.db.entity.AudioEventEntity
import java.io.OutputStream
import java.io.PrintWriter
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class CsvExporter @Inject constructor() {

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

    fun export(events: List<AudioEventEntity>, outputStream: OutputStream) {
        PrintWriter(outputStream).use { writer ->
            writer.println("id,timestamp,eventLabel,confidence,durationMs")
            events.forEach { event ->
                val isoTimestamp = isoFormatter.format(Instant.ofEpochMilli(event.timestamp))
                val escapedLabel = escapeCsvField(event.eventLabel)
                writer.println("${event.id},$isoTimestamp,$escapedLabel,${event.confidence},${event.durationMs}")
            }
        }
    }

    private fun escapeCsvField(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
