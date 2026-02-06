package com.tomst.lolly.fileview

import android.location.Location
import com.tomst.lolly.core.Constants
import com.tomst.lolly.core.TDeviceType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

// Data class automaticky vygeneruje gettery/settery, equals, hashCode a toString
data class FileDetail @JvmOverloads constructor(
    var name: String = "",
    var iconID: Int = 0,
    // Přejmenováno interně, aby nekolidovalo s metodou getFull()
    var internalFullName: String = "",

    // PŘESUNUTO SEM: Aby fungovala metoda .copy(isSelected = ...)
    var isSelected: Boolean = false,
    var isUploaded: Boolean = false
) {
    // --- Properties odpovídající Java polím ---
    var id: Long = 0
    var lidLine: Long = 0
    var location: Location? = null
    var serialNumber: String? = null

    var deviceType: TDeviceType = TDeviceType.dUnknown

    var niceName: String? = null
    var fileSize: Long = 0


    // Přejmenováno, protože Java getter vrací String, ale setter bere LocalDateTime
    // Toto je backing field
    var createdDt: LocalDateTime? = null

    @JvmField var errFlag: Int? = null

    // Statistiky
    // v Kotlinu var automatically generuje getMinT1() a setMinT1()
    var minT1: Double = 0.0
    var maxT1: Double = 0.0
    var minT2: Double = 0.0
    var maxT2: Double = 0.0
    var minT3: Double = 0.0
    var maxT3: Double = 0.0
    var minHum: Double = 0.0
    var maxHum: Double = 0.0

    // Další metadata
    @JvmField var iCount: Long = 0
    @JvmField var iFrom: LocalDateTime? = null
    @JvmField var iInto: LocalDateTime? = null

    // --- Konstruktory pro zachování kompatibility s Javou ---

    constructor(id: Long) : this() {
        this.id = id
        clearMembers()
    }

    constructor(filename: String, fullName: String, iconID: Int) : this(filename, iconID, fullName) {
        clearMembers()
    }

    fun setErr(errFlag: Int) {
        this.errFlag = errFlag
    }

    fun setFrom(from: LocalDateTime?) {
        this.iFrom = from
    }

    fun setInto(into: LocalDateTime?) {
        this.iInto = into
    }

    fun setCount(count: Long) {
        this.iCount = count
    }



    // Java volá .getFull(), v Kotlinu to mapujeme na property internalFullName
    fun getFull(): String = internalFullName
    fun setFull(fullName: String) { this.internalFullName = fullName }

    // Java getCreated() vrací String, ale setCreated bere LocalDateTime.
    // V Kotlinu to musíme rozdělit, protože property nemůže mít různé typy pro get/set.
    fun getCreated(): String {
        return createdDt?.toString() ?: ""
    }

    fun setCreated(created: LocalDateTime?) {
        this.createdDt = created
    }

    // Metoda pro formátované zobrazení (navíc oproti původní Javě, pro Compose)
    fun getFormattedCreated(): String {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        return createdDt?.format(formatter) ?: ""
    }

    // Pomocná metoda, kterou jsi měl v Javě
    private fun clearMembers() {
        this.internalFullName = ""
        this.isSelected = false
        this.isUploaded = false
        this.iCount = 0
        this.iFrom = null
        this.iInto = null
        this.fileSize = 0
    }

    // --- Helper metody pro formátování (Logic moved from Adapter) ---

    fun getFormattedSize(): String {
        var fSize = fileSize.toDouble()
        var suffix = ""
        if (fSize >= 1024) {
            suffix = "KB"
            fSize /= 1024
            if (fSize >= 1024) {
                suffix = "MB"
                fSize /= 1024
                if (fSize >= 1024) {
                    suffix = "GB"
                    fSize /= 1024
                }
            }
        }
        val result = String.format(Locale.US, "%.2f", fSize)
        return if (suffix.isNotEmpty()) "$result $suffix" else result
    }

    fun getDeviceTypeLabel(): String {
        return when (deviceType) {
            TDeviceType.dLolly3 -> "M"
            TDeviceType.dLolly4 -> "M"
            TDeviceType.dAD -> "D"
            TDeviceType.dAdMicro -> "Du"
            TDeviceType.dTermoChron -> "T"
            TDeviceType.dUnknown -> "U"
            else -> "!"
        }
    }

    fun getFormattedFrom(): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return iFrom?.format(fmt) ?: ""
    }

    fun getFormattedInto(): String {
        val fmt = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm")
        return iInto?.format(fmt) ?: ""
    }

    fun getDisplayMinTx(): String {
        val value = when (deviceType) {
            TDeviceType.dLolly3, TDeviceType.dLolly4 -> min(minT1, min(minT2, minT3))
            else -> minT1
        }
        return String.format("%.1f", value)
    }

    fun getDisplayMaxTx(): String {
        val value = when (deviceType) {
            TDeviceType.dLolly3, TDeviceType.dLolly4 -> max(maxT1, max(maxT2, maxT3))
            else -> maxT1
        }
        return String.format("%.1f", value)
    }
}
