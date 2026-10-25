package com.ticketbooking.bookingservice.repository;

import com.ticketbooking.bookingservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentUuid(String paymentUuid);

    Optional<Payment> findByBookingUuid(String bookingUuid);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}