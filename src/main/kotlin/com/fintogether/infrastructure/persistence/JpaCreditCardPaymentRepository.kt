package com.fintogether.infrastructure.persistence

import com.fintogether.domain.entity.CreditCardPayment
import com.fintogether.domain.repository.CreditCardPaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

interface JpaCreditCardPaymentRepositoryInterface : JpaRepository<CreditCardPayment, UUID> {
    fun findByCreditCardIdAndReferenceMonth(creditCardId: UUID, referenceMonth: LocalDate): CreditCardPayment?
    fun findByCreditCardId(creditCardId: UUID): List<CreditCardPayment>
}

@Repository
class JpaCreditCardPaymentRepository(
    private val jpaRepository: JpaCreditCardPaymentRepositoryInterface
) : CreditCardPaymentRepository {

    override fun save(payment: CreditCardPayment): CreditCardPayment =
        jpaRepository.save(payment)

    override fun findByCreditCardIdAndReferenceMonth(cardId: UUID, referenceMonth: LocalDate): CreditCardPayment? =
        jpaRepository.findByCreditCardIdAndReferenceMonth(cardId, referenceMonth)

    override fun findByCreditCardId(cardId: UUID): List<CreditCardPayment> =
        jpaRepository.findByCreditCardId(cardId)

    override fun delete(payment: CreditCardPayment) =
        jpaRepository.delete(payment)
}
