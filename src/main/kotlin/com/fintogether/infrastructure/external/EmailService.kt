package com.fintogether.infrastructure.external

import com.fintogether.domain.exception.EmailSendingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Email service interface for sending various types of emails.
 */
interface EmailService {
    fun sendPasswordResetEmail(to: String, resetToken: String, userName: String)
    fun sendWelcomeEmail(to: String, userName: String)
    fun sendBudgetInvitation(to: String, budgetName: String, inviterName: String)
}

/**
 * Implementation of EmailService using Spring Mail.
 */
@Service
class EmailServiceImpl(
    private val mailSender: JavaMailSender,
    @Value("\${app.email.from:noreply@fintogether.com}")
    private val fromEmail: String,
    @Value("\${app.email.base-url:http://localhost:3000}")
    private val baseUrl: String,
    @Value("\${app.email.enabled:false}")
    private val emailEnabled: Boolean
) : EmailService {

    private val logger = LoggerFactory.getLogger(EmailServiceImpl::class.java)

    @Async
    override fun sendPasswordResetEmail(to: String, resetToken: String, userName: String) {
        if (!emailEnabled) {
            logger.info("Email disabled. Would send password reset email to: $to with token: $resetToken")
            return
        }

        val resetLink = "$baseUrl/reset-password?token=$resetToken"
        val subject = "FinTogether - Password Reset Request"
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>FinTogether</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Hello ${userName},</p>
                        <p>We received a request to reset your password. Click the button below to create a new password:</p>
                        <a href="$resetLink" class="button">Reset Password</a>
                        <p>Or copy and paste this link into your browser:</p>
                        <p><a href="$resetLink">$resetLink</a></p>
                        <p>This link will expire in 1 hour.</p>
                        <p>If you didn't request a password reset, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; ${java.time.Year.now().value} FinTogether. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendEmail(to, subject, htmlContent)
        logger.info("Password reset email sent to: $to")
    }

    @Async
    override fun sendWelcomeEmail(to: String, userName: String) {
        if (!emailEnabled) {
            logger.info("Email disabled. Would send welcome email to: $to")
            return
        }

        val subject = "Welcome to FinTogether!"
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to FinTogether!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello ${userName}!</h2>
                        <p>Thank you for joining FinTogether. We're excited to help you manage your finances!</p>
                        <p>Here are some things you can do:</p>
                        <ul>
                            <li>Create budgets to track your spending</li>
                            <li>Add accounts to monitor your balances</li>
                            <li>Record transactions to stay on top of your finances</li>
                            <li>Share budgets with family members</li>
                        </ul>
                        <a href="$baseUrl/dashboard" class="button">Go to Dashboard</a>
                    </div>
                    <div class="footer">
                        <p>&copy; ${java.time.Year.now().value} FinTogether. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendEmail(to, subject, htmlContent)
        logger.info("Welcome email sent to: $to")
    }

    @Async
    override fun sendBudgetInvitation(to: String, budgetName: String, inviterName: String) {
        if (!emailEnabled) {
            logger.info("Email disabled. Would send budget invitation to: $to for budget: $budgetName")
            return
        }

        val subject = "You've been invited to join a budget on FinTogether"
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
                    .highlight { background-color: #e8e8e8; padding: 10px; border-radius: 4px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Budget Invitation</h1>
                    </div>
                    <div class="content">
                        <h2>You've been invited!</h2>
                        <p><strong>$inviterName</strong> has invited you to collaborate on a budget:</p>
                        <div class="highlight">
                            <strong>$budgetName</strong>
                        </div>
                        <p>Join now to start tracking finances together!</p>
                        <a href="$baseUrl/budgets" class="button">View Invitation</a>
                    </div>
                    <div class="footer">
                        <p>&copy; ${java.time.Year.now().value} FinTogether. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        sendEmail(to, subject, htmlContent)
        logger.info("Budget invitation email sent to: $to for budget: $budgetName")
    }

    private fun sendEmail(to: String, subject: String, htmlContent: String) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(htmlContent, true)

            mailSender.send(message)
        } catch (e: MailException) {
            logger.error("Failed to send email to $to: ${e.message}", e)
            throw EmailSendingException("Failed to send email to $to", e)
        }
    }
}
