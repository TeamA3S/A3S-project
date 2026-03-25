package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
import com.example.a3sproject.domain.portone.webhook.repository.WebhookRepository;
import com.example.a3sproject.domain.subscription.service.SubscriptionWebhookService;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private WebhookRepository webhookRepository;
    @Mock private PaymentService paymentService;
    @Mock private SubscriptionWebhookService subscriptionWebhookService;
    @Spy  private ObjectMapper objectMapper;  // ← 실제 ObjectMapper 주입

    @InjectMocks
    private WebhookService webhookService;

    private String makeRawBody(String transactionId, String paymentId, String status) {
        return """
                {
                  "type": "Transaction.Paid",
                  "data": {
                    "transactionId": "%s",
                    "paymentId": "%s",
                    "status": "%s"
                  }
                }
                """.formatted(transactionId, paymentId, status);
    }

    @Test
    @DisplayName("정상적인 웹훅 수신 시 결제 확정이 호출되고 웹훅 상태가 PROCESSED로 변경된다")
    void handleWebhook_정상수신_결제확정및PROCESSED상태() {
        // given
        given(webhookRepository.existsByWebhookUuid("TXN-001")).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(i -> i.getArgument(0));

        // when
        webhookService.handleWebhook(makeRawBody("TXN-001", "PMN-001", "PAID"));

        // then
        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(captor.capture());
        verify(webhookRepository, atLeastOnce()).save(captor.capture());
        Webhook savedWebhook = captor.getAllValues().get(captor.getAllValues().size() - 1);
        verify(paymentService).confirmPayment("PMN-001", null);

        assertThat(savedWebhook.getStatus()).isEqualTo(WebhookStatus.PROCESSED);
        assertThat(savedWebhook.getWebhookUuid()).isEqualTo("TXN-001");
        assertThat(savedWebhook.getPortOneId()).isEqualTo("PMN-001");
        assertThat(savedWebhook.getEventStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("BillingKey 이벤트는 결제 확정 로직을 호출하지 않는다")
    void handleWebhook_빌링키이벤트_결제확정미호출() {
        // given
        String rawBody = """
                {
                  "type": "BillingKey.Issued",
                  "data": {
                    "billingKey": "billing-key-001",
                    "paymentId": ""
                  }
                }
                """;
        given(webhookRepository.existsByWebhookUuid("billing-key-001")).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(i -> i.getArgument(0));

        // when
        webhookService.handleWebhook(rawBody);

        // then
        verify(paymentService, never()).confirmPayment(anyString(), any());
        verify(subscriptionWebhookService, never()).handleSubscriptionPayment(anyString());
    }

    @Test
    @DisplayName("구독 결제 Transaction.Paid 이벤트는 구독 웹훅 서비스로 전달된다")
    void handleWebhook_구독결제이벤트_구독웹훅호출() {
        // given
        String rawBody = """
                {
                  "type": "Transaction.Paid",
                  "data": {
                    "transactionId": "TXN-SUB-001",
                    "paymentId": "SUB-001",
                    "status": "PAID"
                  }
                }
                """;
        given(webhookRepository.existsByWebhookUuid("TXN-SUB-001")).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(i -> i.getArgument(0));

        // when
        webhookService.handleWebhook(rawBody);

        // then
        verify(subscriptionWebhookService).handleSubscriptionPayment("SUB-001");
        verify(paymentService, never()).confirmPayment(anyString(), any());
    }

    @Test
    @DisplayName("이미 처리된 웹훅 UUID가 재수신되면 중복 처리 없이 즉시 종료된다")
    void handleWebhook_중복웹훅수신_즉시종료() {
        // given
        given(webhookRepository.existsByWebhookUuidAndStatus("TXN-DUP", WebhookStatus.PROCESSED)).willReturn(true);

        // when
        webhookService.handleWebhook(makeRawBody("TXN-DUP", "PMN-DUP", "PAID"));

        // then
        verify(webhookRepository, never()).save(any(Webhook.class));
        verify(paymentService, never()).confirmPayment(anyString(), any());
    }

//    @Test
//    @DisplayName("결제 확정 중 예외가 발생하면 웹훅 상태가 FAILED로 변경된다")
//    void handleWebhook_결제확정중예외발생_FAILED상태() {
//        // given
//        given(webhookRepository.existsByWebhookUuid("TXN-FAIL")).willReturn(false);
//        given(webhookRepository.save(any(Webhook.class))).willAnswer(i -> i.getArgument(0));
//        willThrow(new PaymentException(ErrorCode.PAYMENT_NOT_FOUND))
//                .given(paymentService).confirmPayment("PMN-FAIL", null);
//
//        // when
//        webhookService.handleWebhook(makeRawBody("TXN-FAIL", "PMN-FAIL", "PAID"));
//
//        // then
//        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
//        verify(webhookRepository).save(captor.capture());
//        assertThat(captor.getValue().getStatus()).isEqualTo(WebhookStatus.FAILED);
//    }

    @Test
    @DisplayName("결제 확정 중 예외가 발생해도 웹훅 핸들러는 예외를 외부로 전파하지 않는다")
    void handleWebhook_내부예외발생_예외비전파() {
        // given
        given(webhookRepository.existsByWebhookUuid("TXN-ERR")).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(i -> i.getArgument(0));
        willThrow(new RuntimeException("PortOne 서버 오류"))
                .given(paymentService).confirmPayment("PMN-ERR", null);

        // when & then
        assertThatNoException()
                .isThrownBy(() -> webhookService.handleWebhook(makeRawBody("TXN-ERR", "PMN-ERR", "PAID")));
    }

    @Test
    @DisplayName("웹훅 저장 자체가 실패하면 결제 확정은 호출되지 않는다")
    void handleWebhook_웹훅저장실패_결제확정미호출() {
        // given
        given(webhookRepository.existsByWebhookUuid("TXN-SAVE-FAIL")).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willThrow(new RuntimeException("DB 저장 실패"));

        // when
        webhookService.handleWebhook(makeRawBody("TXN-SAVE-FAIL", "PMN-SAVE-FAIL", "PAID"));

        // then
        verify(paymentService, never()).confirmPayment(anyString(), any());
    }

    @Test
    @DisplayName("웹훅 수신 시 portOneId와 eventStatus가 정확히 웹훅 엔티티에 저장된다")
    void handleWebhook_정상수신_웹훅엔티티필드정확히저장() {
        // given
        given(webhookRepository.existsByWebhookUuid("TXN-FIELD")).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(i -> i.getArgument(0));

        // when
        webhookService.handleWebhook(makeRawBody("TXN-FIELD", "PMN-FIELD-001", "VIRTUAL_ACCOUNT_ISSUED"));

        // then
        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository, atLeastOnce()).save(captor.capture());
        Webhook savedWebhook = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(savedWebhook.getWebhookUuid()).isEqualTo("TXN-FIELD");
        assertThat(savedWebhook.getPortOneId()).isEqualTo("PMN-FIELD-001");
        assertThat(savedWebhook.getEventStatus()).isEqualTo("VIRTUAL_ACCOUNT_ISSUED");
        assertThat(savedWebhook.getStatus()).isEqualTo(WebhookStatus.PROCESSED);
    }
}