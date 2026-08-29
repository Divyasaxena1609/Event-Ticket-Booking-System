package com.ticketbooking.bookingservice.service;

import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.ticketbooking.bookingservice.dto.response.CreatePaymentResponse;
import com.ticketbooking.bookingservice.entity.Booking;
import com.ticketbooking.bookingservice.entity.BookingSeat;
import com.ticketbooking.bookingservice.entity.BookingStatus;
import com.ticketbooking.bookingservice.entity.Payment;
import com.ticketbooking.bookingservice.entity.PaymentStatus;
import com.ticketbooking.bookingservice.repository.BookingRepository;
import com.ticketbooking.bookingservice.repository.BookingSeatRepository;
import com.ticketbooking.bookingservice.repository.PaymentRepository;
import com.ticketbooking.bookingservice.service.Impl.PaymentServiceImpl;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSeatRepository bookingSeatRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ISeatLockService seatLockService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "key", "test_key_123");
        ReflectionTestUtils.setField(paymentService, "secret", "test_secret_123");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "test_webhook_secret_123");
    }

    @Test
    void testCreateOrder_CreatesPaymentWithoutUnsupportedExpiryField() throws Exception {
        Booking booking = new Booking();
        booking.setBookingUUID("booking-100");
        booking.setTotalAmount(new BigDecimal("1500.00"));
        booking.setStatus(BookingStatus.CREATED);

        when(bookingRepository.findByBookingUUID("booking-100")).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingUuid("booking-100")).thenReturn(Optional.empty());

        OrderClient mockOrders = mock(OrderClient.class);
        razorpayClient.orders = mockOrders;

        JSONObject mockOrderJson = new JSONObject();
        mockOrderJson.put("id", "order_rzp_999");
        mockOrderJson.put("amount", 150000);
        mockOrderJson.put("currency", "INR");

        when(mockOrders.create(any(JSONObject.class))).thenAnswer(invocation -> {
            JSONObject req = invocation.getArgument(0);
            assertFalse(req.has("expire_by"));
            return new Order(mockOrderJson);
        });

        CreatePaymentResponse response = paymentService.createOrder("booking-100");

        assertNotNull(response);
        assertEquals("order_rzp_999", response.getOrderId());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testProcessWebhook_OrderPaid_ConfirmsBookingAndReleasesLock() throws Exception {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_123");
        payment.setBookingUuid("booking-100");
        payment.setStatus(PaymentStatus.PENDING);

        Booking booking = new Booking();
        booking.setBookingUUID("booking-100");
        booking.setEventUuid("event-abc");
        booking.setStatus(BookingStatus.CREATED);

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingUUID("booking-100")).thenReturn(Optional.of(booking));

        String webhookJson = """
        {
          "event": "order.paid",
          "payload": {
            "order": {
              "entity": {
                "id": "order_123"
              }
            },
            "payment": {
              "entity": {
                "id": "pay_xyz",
                "order_id": "order_123"
              }
            }
          }
        }
        """;

        paymentService.processWebhook(webhookJson, signWebhookPayload(webhookJson));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(seatLockService).releaseBookingLocks("event-abc", "booking-100");
        verify(bookingRepository).save(booking);
    }

    @Test
    void testProcessWebhook_RejectsMissingSignature() {
        assertThrows(SecurityException.class, () -> paymentService.processWebhook("{}", null));
        verifyNoInteractions(paymentRepository, bookingRepository, bookingSeatRepository, seatLockService);
    }

    @Test
    void testProcessWebhook_PaymentFailed_ImmediatelyReleasesSeatLock() throws Exception {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_123");
        payment.setBookingUuid("booking-100");
        payment.setStatus(PaymentStatus.PENDING);

        Booking booking = new Booking();
        booking.setBookingUUID("booking-100");
        booking.setEventUuid("event-abc");
        booking.setStatus(BookingStatus.CREATED);

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingUUID("booking-100")).thenReturn(Optional.of(booking));

        String webhookJson = """
        {
          "event": "payment.failed",
          "payload": {
            "payment": {
              "entity": {
                "id": "pay_failed_1",
                "order_id": "order_123"
              }
            }
          }
        }
        """;

        paymentService.processWebhook(webhookJson, signWebhookPayload(webhookJson));

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(BookingStatus.FAILED, booking.getStatus());
        verify(seatLockService).releaseBookingLocks("event-abc", "booking-100");
        verify(bookingSeatRepository).deleteByBookingUUID("booking-100");
        verify(bookingRepository).save(booking);
    }

    @Test
    void testProcessWebhook_LatePaymentOnTakenSeat_TriggersAutoRefund() throws Exception {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_123");
        payment.setBookingUuid("booking-100");
        payment.setAmount(new BigDecimal("500.00"));
        payment.setStatus(PaymentStatus.PENDING);

        Booking booking = new Booking();
        booking.setBookingUUID("booking-100");
        booking.setEventUuid("event-abc");
        booking.setStatus(BookingStatus.EXPIRED); // Was expired after 12 mins

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingUUID("booking-100")).thenReturn(Optional.of(booking));

        BookingSeat seat1 = new BookingSeat(1L, "booking-100", "A1", new BigDecimal("500.00"), "event-abc");
        when(bookingSeatRepository.findByBookingUUID("booking-100")).thenReturn(Collections.singletonList(seat1));

        // Another booking has confirmed seat A1
        Booking conflictingBooking = new Booking();
        conflictingBooking.setBookingUUID("booking-200");
        conflictingBooking.setStatus(BookingStatus.CONFIRMED);
        BookingSeat conflictingSeat = new BookingSeat(2L, "booking-200", "A1", new BigDecimal("500.00"), "event-abc");

        when(bookingRepository.findByEventUuid("event-abc")).thenReturn(Collections.singletonList(conflictingBooking));
        when(bookingSeatRepository.findByBookingUUID("booking-200")).thenReturn(Collections.singletonList(conflictingSeat));

        PaymentClient mockPayments = mock(PaymentClient.class);
        razorpayClient.payments = mockPayments;

        String webhookJson = """
        {
          "event": "payment.captured",
          "payload": {
            "payment": {
              "entity": {
                "id": "pay_late_123",
                "order_id": "order_123"
              }
            }
          }
        }
        """;

        paymentService.processWebhook(webhookJson, signWebhookPayload(webhookJson));

        verify(mockPayments).refund(eq("pay_late_123"), any(JSONObject.class));
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(BookingStatus.REFUNDED, booking.getStatus());
    }

    @Test
    void testFailPayment_ExplicitCall_SetsFailedAndReleasesSeats() throws Exception {
        Payment payment = new Payment();
        payment.setRazorpayOrderId("order_123");
        payment.setBookingUuid("booking-100");
        payment.setStatus(PaymentStatus.PENDING);

        Booking booking = new Booking();
        booking.setBookingUUID("booking-100");
        booking.setEventUuid("event-abc");
        booking.setStatus(BookingStatus.CREATED);

        when(paymentRepository.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(payment));
        when(bookingRepository.findByBookingUUID("booking-100")).thenReturn(Optional.of(booking));

        paymentService.failPayment("booking-100", "order_123", "Card declined by issuing bank");

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(BookingStatus.FAILED, booking.getStatus());
        verify(seatLockService).releaseBookingLocks("event-abc", "booking-100");
        verify(bookingSeatRepository).deleteByBookingUUID("booking-100");
        verify(bookingRepository).save(booking);
        verify(paymentRepository).save(payment);
    }

    private String signWebhookPayload(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test_webhook_secret_123".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder signature = new StringBuilder();
        for (byte value : digest) {
            signature.append(String.format("%02x", value));
        }
        return signature.toString();
    }
}
