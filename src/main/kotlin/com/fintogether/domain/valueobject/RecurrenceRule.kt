package com.fintogether.domain.valueobject

import com.fintogether.domain.entity.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Value object representing a recurrence rule for recurring transactions.
 * Parsed from a JSON map stored in the database.
 */
class RecurrenceRule private constructor(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val dayOfMonth: Int?,
    val dayOfWeek: DayOfWeek?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val occurrenceCount: Int?
) {

    /**
     * Computes the next occurrence date after the given date.
     * Returns null if the recurrence has expired or there are no more occurrences.
     */
    fun computeNextOccurrence(fromDate: LocalDate): LocalDate? {
        val nextDate = when (frequency) {
            RecurrenceFrequency.MONTHLY -> computeNextMonthly(fromDate)
            RecurrenceFrequency.WEEKLY -> computeNextWeekly(fromDate)
            RecurrenceFrequency.YEARLY -> computeNextYearly(fromDate)
        }

        // If endDate is set and next date exceeds it, no more occurrences
        if (nextDate != null && endDate != null && nextDate.isAfter(endDate)) {
            return null
        }

        return nextDate
    }

    /**
     * Checks whether the recurrence is expired based on the current date
     * and the number of transactions already generated.
     */
    fun isExpired(currentDate: LocalDate, generatedCount: Int): Boolean {
        if (endDate != null && currentDate.isAfter(endDate)) {
            return true
        }
        if (occurrenceCount != null && generatedCount >= occurrenceCount) {
            return true
        }
        return false
    }

    /**
     * Converts this RecurrenceRule back to a Map for JSON storage.
     */
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "frequency" to frequency.name,
            "interval" to interval,
            "startDate" to startDate.toString()
        )
        dayOfMonth?.let { map["dayOfMonth"] = it }
        dayOfWeek?.let { map["dayOfWeek"] = it.name }
        endDate?.let { map["endDate"] = it.toString() }
        occurrenceCount?.let { map["occurrenceCount"] = it }
        return map
    }

    private fun computeNextMonthly(fromDate: LocalDate): LocalDate {
        val targetDay = dayOfMonth ?: fromDate.dayOfMonth
        val nextMonth = fromDate.plusMonths(interval.toLong())
        val yearMonth = YearMonth.of(nextMonth.year, nextMonth.month)
        val clampedDay = minOf(targetDay, yearMonth.lengthOfMonth())
        return LocalDate.of(nextMonth.year, nextMonth.month, clampedDay)
    }

    private fun computeNextWeekly(fromDate: LocalDate): LocalDate {
        return fromDate.plusWeeks(interval.toLong())
    }

    private fun computeNextYearly(fromDate: LocalDate): LocalDate {
        val nextYear = fromDate.plusYears(interval.toLong())
        // Handle leap year: if original was Feb 29 and target year doesn't have it, clamp to Feb 28
        val yearMonth = YearMonth.of(nextYear.year, nextYear.month)
        val clampedDay = minOf(fromDate.dayOfMonth, yearMonth.lengthOfMonth())
        return LocalDate.of(nextYear.year, nextYear.month, clampedDay)
    }

    companion object {
        /**
         * Parses a RecurrenceRule from a Map (typically deserialized from JSON).
         */
        fun fromMap(map: Map<String, Any>): RecurrenceRule {
            val frequencyStr = map["frequency"] as? String
                ?: throw IllegalArgumentException("RecurrenceRule requires 'frequency' field")

            val frequency = try {
                RecurrenceFrequency.valueOf(frequencyStr.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid frequency: $frequencyStr. Must be one of: ${RecurrenceFrequency.entries.joinToString()}")
            }

            val interval = when (val raw = map["interval"]) {
                is Number -> raw.toInt()
                is String -> raw.toInt()
                else -> 1
            }

            val dayOfMonth = when (val raw = map["dayOfMonth"]) {
                is Number -> raw.toInt()
                is String -> raw.toInt()
                else -> null
            }

            val dayOfWeek = when (val raw = map["dayOfWeek"]) {
                is String -> try { DayOfWeek.valueOf(raw.uppercase()) } catch (_: Exception) { null }
                else -> null
            }

            val startDateStr = map["startDate"] as? String
                ?: throw IllegalArgumentException("RecurrenceRule requires 'startDate' field")
            val startDate = LocalDate.parse(startDateStr)

            val endDate = (map["endDate"] as? String)?.let { LocalDate.parse(it) }

            val occurrenceCount = when (val raw = map["occurrenceCount"]) {
                is Number -> raw.toInt()
                is String -> raw.toInt()
                else -> null
            }

            require(interval > 0) { "Interval must be positive" }

            return RecurrenceRule(
                frequency = frequency,
                interval = interval,
                dayOfMonth = dayOfMonth,
                dayOfWeek = dayOfWeek,
                startDate = startDate,
                endDate = endDate,
                occurrenceCount = occurrenceCount
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecurrenceRule) return false
        return frequency == other.frequency &&
                interval == other.interval &&
                dayOfMonth == other.dayOfMonth &&
                dayOfWeek == other.dayOfWeek &&
                startDate == other.startDate &&
                endDate == other.endDate &&
                occurrenceCount == other.occurrenceCount
    }

    override fun hashCode(): Int {
        var result = frequency.hashCode()
        result = 31 * result + interval
        result = 31 * result + (dayOfMonth ?: 0)
        result = 31 * result + (dayOfWeek?.hashCode() ?: 0)
        result = 31 * result + startDate.hashCode()
        result = 31 * result + (endDate?.hashCode() ?: 0)
        result = 31 * result + (occurrenceCount ?: 0)
        return result
    }

    override fun toString(): String {
        return "RecurrenceRule(frequency=$frequency, interval=$interval, dayOfMonth=$dayOfMonth, dayOfWeek=$dayOfWeek, startDate=$startDate, endDate=$endDate, occurrenceCount=$occurrenceCount)"
    }
}
