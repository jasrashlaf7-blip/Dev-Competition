package com.olympiades.firststep.domain.export

import com.olympiades.firststep.domain.model.ApplicationWithDetails
import com.olympiades.firststep.domain.model.DateTimeHelper

object CsvExporter {
    private val DANGEROUS_PREFIXES = charArrayOf('=', '+', '-', '@', '\t', '\r')

    /**
     * Neutralizes spreadsheet formula injection (OWASP CSV Injection defense)
     * and applies RFC 4180 quoting.
     */
    fun sanitizeCsvCell(raw: String): String {
        val cleaned = raw
            .replace("\r\n", " ")
            .replace('\n', ' ')
            .replace('\r', ' ')
        val trimmedLeading = cleaned.trimStart()
        val needsPrefix = cleaned.firstOrNull()?.let { it in DANGEROUS_PREFIXES } == true ||
            trimmedLeading.firstOrNull()?.let { it in DANGEROUS_PREFIXES } == true
        val safeValue = if (needsPrefix) "'$cleaned" else cleaned
        val escaped = safeValue.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun buildApplicationsCsv(
        applications: List<ApplicationWithDetails>,
        todayIso: String = DateTimeHelper.todayIso()
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM for clean French & Arabic rendering in Excel
        sb.append('\uFEFF')
        sb.appendLine(
            listOf(
                "Reference",
                "Offer_Title",
                "Company",
                "Domain",
                "City",
                "Monthly_Stipend_DZD",
                "Start_Date",
                "Applicant_Name",
                "Applicant_Email",
                "Applicant_Phone",
                "Status",
                "Applied_At",
                "Unanswered_Over_7_Days",
                "Cover_Letter"
            ).joinToString(",")
        )
        applications.forEach { app ->
            val overdue = if (app.isUnansweredOver7Days(todayIso)) "YES" else "NO"
            val row = listOf(
                app.reference,
                app.offer.titleFr,
                app.offer.company.name,
                app.offer.domain.nameFr,
                app.offer.location.cityFr,
                app.offer.monthlyStipendDzd.toString(),
                app.offer.startDate,
                app.applicantName,
                app.applicantEmail,
                app.applicantPhone,
                app.currentStatus.name,
                app.appliedAt,
                overdue,
                app.coverLetter
            ).joinToString(",") { sanitizeCsvCell(it) }
            sb.appendLine(row)
        }
        return sb.toString()
    }
}
