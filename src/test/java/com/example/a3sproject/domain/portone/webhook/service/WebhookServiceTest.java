package com.example.a3sproject.domain.portone.webhook.service;

import com.example.a3sproject.domain.payment.service.PaymentService;
import com.example.a3sproject.domain.portone.webhook.entity.Webhook;
import com.example.a3sproject.domain.portone.webhook.enums.WebhookStatus;
import com.example.a3sproject.domain.portone.webhook.repository.WebhookRepository;
import com.example.a3sproject.global.exception.common.ErrorCode;
import com.example.a3sproject.global.exception.domain.PaymentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock private WebhookRepository webhookRepository;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private WebhookService webhookService;

    private Webhook createWebhook(String webhookUuid, String portOneId, String eventStatus) {
        Webhook webhook = new Webhook(webhookUuid, portOneId, eventStatus);
        ReflectionTestUtils.setField(webhook, "id", 1L);
        return webhook;
    }

    @Test
    @DisplayName("정상적인 웹훅 수신 시 결제 확정이 호출되고 웹훅 상태가 PROCESSED로 변경된다")
    void handleWebhook_정상수신_결제확정및PROCESSED상태() {
        // given: 중복되지 않은 신규 웹훅, 결제 확정 정상 처리
        given(webhookRepository.existsByWebhookUuidAndStatus("WH-001", WebhookStatus.PROCESSED)).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        webhookService.handleWebhook("WH-001", "PMN-001", "PAID");

        // then: 웹훅 저장과 결제 확정이 순서대로 호출되고 상태가 PROCESSED로 변경된다
        ArgumentCaptor<Webhook> webhookCaptor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        verify(paymentService).confirmPayment("PMN-001", null);

        assertThat(webhookCaptor.getValue().getStatus()).isEqualTo(WebhookStatus.PROCESSED);
        assertThat(webhookCaptor.getValue().getPortOneId()).isEqualTo("PMN-001");
        assertThat(webhookCaptor.getValue().getEventStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("이미 PROCESSED 처리된 웹훅 UUID가 재수신되면 중복 처리 없이 즉시 종료된다")
    void handleWebhook_중복웹훅수신_즉시종료() {
        // given: 동일한 webhookUuid가 이미 PROCESSED 상태로 존재
        given(webhookRepository.existsByWebhookUuidAndStatus("WH-DUP", WebhookStatus.PROCESSED)).willReturn(true);

        // when
        webhookService.handleWebhook("{data:{WH-DUP, PMN-001, PAID}}");

        // then: 웹훅 저장과 결제 확정이 모두 호출되지 않는다
        verify(webhookRepository, never()).save(any(Webhook.class));
        verify(paymentService, never()).confirmPayment(anyString(), any());
    }

    @Test
    @DisplayName("결제 확정 중 예외가 발생하면 웹훅 상태가 FAILED로 변경된다")
    void handleWebhook_결제확정중예외발생_FAILED상태() {
        // given: 결제 확정 시 PaymentException 발생 (결제 정보 없음 등)
        given(webhookRepository.existsByWebhookUuidAndStatus("WH-FAIL", WebhookStatus.PROCESSED)).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(invocation -> invocation.getArgument(0));
        willThrow(new PaymentException(ErrorCode.PAYMENT_NOT_FOUND))
                .given(paymentService).confirmPayment("PMN-FAIL", null);

        // when
        webhookService.handleWebhook("WH-FAIL", "PMN-FAIL", "PAID");

        // then: 예외가 외부로 전파되지 않고 웹훅 상태만 FAILED로 변경된다
        ArgumentCaptor<Webhook> webhookCaptor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        assertThat(webhookCaptor.getValue().getStatus()).isEqualTo(WebhookStatus.FAILED);
    }

    @Test
    @DisplayName("결제 확정 중 예외가 발생해도 웹훅 핸들러는 예외를 외부로 전파하지 않는다")
    void handleWebhook_내부예외발생_예외비전파() {
        // given: 결제 확정 시 런타임 예외 발생 (PortOne 서버 오류 등)
        // 웹훅 컨트롤러에서 PortOne 재전송 방지를 위해 200 OK를 반환해야 하므로
        // 예외가 외부로 새어나가면 안 된다
        given(webhookRepository.existsByWebhookUuidAndStatus("WH-ERR", WebhookStatus.PROCESSED)).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(invocation -> invocation.getArgument(0));
        willThrow(new RuntimeException("PortOne 서버 오류"))
                .given(paymentService).confirmPayment("PMN-ERR", null);

        // when & then: handleWebhook 호출 시 어떤 예외도 발생하지 않는다
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> webhookService.handleWebhook("WH-ERR", "PMN-ERR", "PAID"));
    }

    @Test
    @DisplayName("웹훅 저장 자체가 실패하면 결제 확정은 호출되지 않는다")
    void handleWebhook_웹훅저장실패_결제확정미호출() {
        // given: webhookRepository.save() 자체에서 예외 발생 (DB 오류 등)
        given(webhookRepository.existsByWebhookUuidAndStatus("WH-SAVE-FAIL", WebhookStatus.PROCESSED)).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willThrow(new RuntimeException("DB 저장 실패"));

        // when
        webhookService.handleWebhook("WH-SAVE-FAIL", "PMN-001", "PAID");

        // then: 웹훅 저장이 실패했으므로 결제 확정은 호출되지 않는다
        verify(paymentService, never()).confirmPayment(anyString(), any());
    }

    @Test
    @DisplayName("웹훅 수신 시 portOneId와 eventStatus가 정확히 웹훅 엔티티에 저장된다")
    void handleWebhook_정상수신_웹훅엔티티필드정확히저장() {
        // given: 정상 웹훅 수신
        given(webhookRepository.existsByWebhookUuidAndStatus("WH-FIELD", WebhookStatus.PROCESSED)).willReturn(false);
        given(webhookRepository.save(any(Webhook.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        webhookService.handleWebhook("WH-FIELD", "PMN-FIELD-001", "VIRTUAL_ACCOUNT_ISSUED");

        // then: 저장된 웹훅 엔티티의 모든 필드가 수신된 값과 정확히 일치한다
        ArgumentCaptor<Webhook> webhookCaptor = ArgumentCaptor.forClass(Webhook.class);
        verify(webhookRepository).save(webhookCaptor.capture());
        assertThat(webhookCaptor.getValue().getWebhookUuid()).isEqualTo("WH-FIELD");
        assertThat(webhookCaptor.getValue().getPortOneId()).isEqualTo("PMN-FIELD-001");
        assertThat(webhookCaptor.getValue().getEventStatus()).isEqualTo("VIRTUAL_ACCOUNT_ISSUED");
        assertThat(webhookCaptor.getValue().getStatus()).isEqualTo(WebhookStatus.PROCESSED);
    }
}