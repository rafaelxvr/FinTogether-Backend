package com.fintogether.domain.repository

import com.fintogether.domain.entity.CreditCardPayment
import java.time.LocalDate
import java.util.UUID

interface CreditCardPaymentRepository {
    fun save(payment: CreditCardPayment): CreditCardPayment
    fun findByCreditCardIdAndReferenceMonth(cardId: UUID, referenceMonth: LocalDate): CreditCardPayment?
    fun findByCreditCardId(cardId: UUID): List<CreditCardPayment>
    fun delete(payment: CreditCardPayment)
}
