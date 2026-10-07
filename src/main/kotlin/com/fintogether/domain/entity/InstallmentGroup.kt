package com.fintogether.domain.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "installment_groups")
class InstallmentGroup(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    val budget: Budget,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    val card: CreditCard? = null,

    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    var totalAmount: BigDecimal,

    @Column(name = "installment_count", nullable = false)
    var installmentCount: Int,

    @Column(name = "interest_rate", precision = 5, scale = 2, nullable = false)
    var interestRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "description")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: InstallmentGroupStatus = InstallmentGroupStatus.ACTIVE,

    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.now()
) {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    val id: UUID = id

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = createdAt

    init {
        validateInstallmentCount()
        validateInterestRate()
    }

    private fun validateInstallmentCount() {
        require(installmentCount > 0) {
            "Installment count must be greater than zero"
        }
    }

    private fun validateInterestRate() {
        require(interestRate >= BigDecimal.ZERO) {
            "Interest rate cannot be negative"
        }
    }

    fun calculateInstallmentAmount(): BigDecimal {
        if (interestRate == BigDecimal.ZERO) {
            return totalAmount.divide(BigDecimal(installmentCount), 2, java.math.RoundingMode.HALF_UP)
        }

        // Calculate with interest using compound interest formula
        val monthlyRate = interestRate.divide(BigDecimal(100), 6, java.math.RoundingMode.HALF_UP)
        val onePlusRate = BigDecimal.ONE.add(monthlyRate)
        val powerResult = onePlusRate.pow(installmentCount)

        val numerator = totalAmount.multiply(monthlyRate).multiply(powerResult)
        val denominator = powerResult.subtract(BigDecimal.ONE)

        return numerator.divide(denominator, 2, java.math.RoundingMode.HALF_UP)
    }

    fun calculateTotalWithInterest(): BigDecimal {
        if (interestRate == BigDecimal.ZERO) {
            return totalAmount
        }
        return calculateInstallmentAmount().multiply(BigDecimal(installmentCount))
    }

    fun update(
        totalAmount: BigDecimal? = null,
        installmentCount: Int? = null,
        interestRate: BigDecimal? = null,
        description: String? = null
    ) {
        totalAmount?.let {
            this.totalAmount = it
        }
        installmentCount?.let {
            this.installmentCount = it
            validateInstallmentCount()
        }
        interestRate?.let {
            this.interestRate = it
            validateInterestRate()
        }
        description?.let { this.description = it }
    }

    fun markAsCompleted() {
        this.status = InstallmentGroupStatus.COMPLETED
    }

    fun cancel() {
        this.status = InstallmentGroupStatus.CANCELLED
    }

    fun isActive(): Boolean = status == InstallmentGroupStatus.ACTIVE

    fun hasInterest(): Boolean = interestRate > BigDecimal.ZERO

    fun belongsToCard(): Boolean = card != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstallmentGroup) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "InstallmentGroup(id=$id, totalAmount=$totalAmount, count=$installmentCount, status=$status, description=$description)"
    }
}

enum class InstallmentGroupStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
