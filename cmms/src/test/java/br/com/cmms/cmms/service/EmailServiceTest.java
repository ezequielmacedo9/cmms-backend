package br.com.cmms.cmms.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link EmailService} (HTML templates + fail-soft send). */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    @InjectMocks EmailService service;

    private MimeMessage message;

    @BeforeEach
    void setUp() {
        message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@cmms.app");
        ReflectionTestUtils.setField(service, "frontendUrl", "https://app.cmms.test");
    }

    @Test
    @DisplayName("sendPasswordReset: monta HTML com nome e link com token e envia")
    void sendPasswordReset_envia() throws Exception {
        service.sendPasswordReset("ana@cmms.app", "TOKEN123", "Ana");

        verify(mailSender).send(message);
        assertThat(message.getSubject()).contains("Redefinição de Senha");
        String html = extractHtml(message.getContent());
        assertThat(html).contains("Ana");
        assertThat(html).contains("https://app.cmms.test/reset-password?token=TOKEN123");
    }

    @Test
    @DisplayName("sendWelcome: inclui credenciais e usa email como nome quando nome e nulo")
    void sendWelcome_envia() throws Exception {
        service.sendWelcome("novo@cmms.app", null, "SenhaTemp1");

        verify(mailSender).send(message);
        String html = extractHtml(message.getContent());
        assertThat(html).contains("novo@cmms.app");
        assertThat(html).contains("SenhaTemp1");
    }

    /** MimeMessageHelper(multipart=true) nests the HTML inside MimeMultipart parts. */
    private static String extractHtml(Object content) throws Exception {
        if (content instanceof String s) return s;
        if (content instanceof MimeMultipart mp) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                sb.append(extractHtml(mp.getBodyPart(i).getContent()));
            }
            return sb.toString();
        }
        return "";
    }

    @Test
    @DisplayName("falha de SMTP nao propaga (fail-soft, apenas loga)")
    void falhaSmtp_naoPropaga() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> service.sendPasswordReset("ana@cmms.app", "T", "Ana"))
            .doesNotThrowAnyException();
    }
}
