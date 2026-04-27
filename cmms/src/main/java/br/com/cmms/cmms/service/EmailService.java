package br.com.cmms.cmms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@cmms.app}")
    private String fromEmail;

    @Value("${app.frontend.url:https://cmms-frontend.vercel.app}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordReset(String toEmail, String token, String nome) {
        String link = frontendUrl + "/reset-password?token=" + token;
        String html = buildPasswordResetEmail(nome != null ? nome : toEmail, link);
        sendHtml(toEmail, "🔐 Redefinição de Senha — CMMS", html);
    }

    public void sendWelcome(String toEmail, String nome, String senha) {
        String html = buildWelcomeEmail(nome != null ? nome : toEmail, toEmail, senha);
        sendHtml(toEmail, "🎉 Bem-vindo ao CMMS Industrial Suite", html);
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "CMMS Industrial Suite");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildPasswordResetEmail(String nome, String link) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8">
            <style>
              body { margin:0; font-family: 'Segoe UI', Arial, sans-serif; background:#0d0d1a; }
              .container { max-width:540px; margin:40px auto; background:#12122a; border-radius:16px;
                           border:1px solid rgba(139,92,246,0.2); overflow:hidden; }
              .header { background:linear-gradient(135deg,#1e1b4b,#4c1d95); padding:32px; text-align:center; }
              .logo { font-size:24px; font-weight:700; color:#fff; letter-spacing:-0.02em; }
              .logo-sub { font-size:11px; color:rgba(255,255,255,0.5); margin-top:4px; }
              .body { padding:32px; }
              h2 { color:#fafafa; font-size:20px; margin:0 0 16px; }
              p { color:rgba(255,255,255,0.65); font-size:14px; line-height:1.6; margin:0 0 16px; }
              .btn { display:inline-block; background:#7c3aed; color:#fff; text-decoration:none;
                     padding:12px 28px; border-radius:8px; font-weight:600; font-size:14px;
                     margin:8px 0 24px; }
              .note { font-size:12px; color:rgba(255,255,255,0.3); }
              .footer { border-top:1px solid rgba(255,255,255,0.06); padding:20px 32px;
                        font-size:11px; color:rgba(255,255,255,0.2); text-align:center; }
            </style></head>
            <body>
            <div class="container">
              <div class="header">
                <div class="logo">CMMS</div>
                <div class="logo-sub">Industrial Suite</div>
              </div>
              <div class="body">
                <h2>Olá, %s!</h2>
                <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                <p>Clique no botão abaixo para criar uma nova senha:</p>
                <a href="%s" class="btn">Redefinir Senha</a>
                <p class="note">Este link expira em <strong>1 hora</strong>. Se você não solicitou a troca de senha, ignore este email.</p>
              </div>
              <div class="footer">CMMS Industrial Suite · Este é um email automático, não responda.</div>
            </div>
            </body></html>
            """.formatted(nome, link);
    }

    private String buildWelcomeEmail(String nome, String email, String senha) {
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8">
            <style>
              body { margin:0; font-family: 'Segoe UI', Arial, sans-serif; background:#0d0d1a; }
              .container { max-width:540px; margin:40px auto; background:#12122a; border-radius:16px;
                           border:1px solid rgba(139,92,246,0.2); overflow:hidden; }
              .header { background:linear-gradient(135deg,#1e1b4b,#4c1d95); padding:32px; text-align:center; }
              .logo { font-size:24px; font-weight:700; color:#fff; letter-spacing:-0.02em; }
              .body { padding:32px; }
              h2 { color:#fafafa; font-size:20px; margin:0 0 16px; }
              p { color:rgba(255,255,255,0.65); font-size:14px; line-height:1.6; margin:0 0 12px; }
              .cred { background:rgba(124,58,237,0.08); border:1px solid rgba(124,58,237,0.2);
                      border-radius:8px; padding:16px; margin:16px 0; }
              .cred-row { display:flex; justify-content:space-between; margin-bottom:8px; }
              .cred-label { color:rgba(255,255,255,0.4); font-size:12px; }
              .cred-value { color:#c4b5fd; font-weight:600; font-size:13px; }
              .footer { border-top:1px solid rgba(255,255,255,0.06); padding:20px 32px;
                        font-size:11px; color:rgba(255,255,255,0.2); text-align:center; }
            </style></head>
            <body>
            <div class="container">
              <div class="header">
                <div class="logo">CMMS</div>
              </div>
              <div class="body">
                <h2>Bem-vindo, %s!</h2>
                <p>Sua conta foi criada no CMMS Industrial Suite. Aqui estão suas credenciais de acesso:</p>
                <div class="cred">
                  <div class="cred-row"><span class="cred-label">Email</span><span class="cred-value">%s</span></div>
                  <div class="cred-row"><span class="cred-label">Senha temporária</span><span class="cred-value">%s</span></div>
                </div>
                <p>Por segurança, altere sua senha após o primeiro acesso.</p>
              </div>
              <div class="footer">CMMS Industrial Suite · Email automático</div>
            </div>
            </body></html>
            """.formatted(nome, email, senha);
    }
}
