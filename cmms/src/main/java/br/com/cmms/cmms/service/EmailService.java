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

    public void sendManutencaoVencida(String toEmail, String maquinaNome, String setor, long diasVencido) {
        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:#0d0d1a;}
              .c{max-width:540px;margin:40px auto;background:#12122a;border-radius:16px;border:1px solid rgba(239,68,68,.3);}
              .h{background:linear-gradient(135deg,#7f1d1d,#991b1b);padding:28px;text-align:center;}
              .logo{font-size:22px;font-weight:700;color:#fff;}
              .b{padding:28px;}
              h2{color:#fca5a5;font-size:18px;margin:0 0 12px;}
              p{color:rgba(255,255,255,.65);font-size:14px;line-height:1.6;margin:0 0 12px;}
              .badge{display:inline-block;background:rgba(239,68,68,.15);border:1px solid rgba(239,68,68,.4);
                     color:#fca5a5;padding:6px 14px;border-radius:20px;font-weight:600;font-size:13px;}
              .f{border-top:1px solid rgba(255,255,255,.06);padding:16px 28px;font-size:11px;color:rgba(255,255,255,.2);text-align:center;}
            </style></head><body>
            <div class="c">
              <div class="h"><div class="logo">⚠ CMMS — Alerta de Manutenção</div></div>
              <div class="b">
                <h2>Manutenção Preventiva Vencida</h2>
                <p>A máquina <strong style="color:#fca5a5">%s</strong> (Setor: %s) está com a manutenção preventiva vencida há <span class="badge">%d dia(s)</span>.</p>
                <p>Uma ordem de serviço foi gerada automaticamente. Verifique o sistema para atribuir um técnico.</p>
              </div>
              <div class="f">CMMS Industrial Suite · Alerta automático</div>
            </div></body></html>
            """.formatted(maquinaNome, setor, diasVencido);
        sendHtml(toEmail, "⚠ Manutenção Preventiva Vencida — " + maquinaNome, html);
    }

    public void sendSlaVencendo(String toEmail, String maquinaNome, String tipo, long diasRestantes) {
        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:#0d0d1a;}
              .c{max-width:540px;margin:40px auto;background:#12122a;border-radius:16px;border:1px solid rgba(234,179,8,.3);}
              .h{background:linear-gradient(135deg,#713f12,#92400e);padding:28px;text-align:center;}
              .logo{font-size:22px;font-weight:700;color:#fff;}
              .b{padding:28px;}
              h2{color:#fde68a;font-size:18px;margin:0 0 12px;}
              p{color:rgba(255,255,255,.65);font-size:14px;line-height:1.6;margin:0 0 12px;}
              .badge{display:inline-block;background:rgba(234,179,8,.15);border:1px solid rgba(234,179,8,.4);
                     color:#fde68a;padding:6px 14px;border-radius:20px;font-weight:600;font-size:13px;}
              .f{border-top:1px solid rgba(255,255,255,.06);padding:16px 28px;font-size:11px;color:rgba(255,255,255,.2);text-align:center;}
            </style></head><body>
            <div class="c">
              <div class="h"><div class="logo">🕐 CMMS — SLA Vencendo</div></div>
              <div class="b">
                <h2>Prazo de SLA Próximo do Vencimento</h2>
                <p>A ordem de serviço <strong style="color:#fde68a">%s</strong> na máquina <strong>%s</strong> vence em <span class="badge">%d dia(s)</span>.</p>
                <p>Acesse o sistema para tomar as ações necessárias antes do prazo.</p>
              </div>
              <div class="f">CMMS Industrial Suite · Alerta automático</div>
            </div></body></html>
            """.formatted(tipo, maquinaNome, diasRestantes);
        sendHtml(toEmail, "🕐 SLA Vencendo — " + maquinaNome, html);
    }

    public void sendEstoqueBaixo(String toEmail, String pecaNome, String codigo, int qtdAtual, int qtdMinima) {
        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:#0d0d1a;}
              .c{max-width:540px;margin:40px auto;background:#12122a;border-radius:16px;border:1px solid rgba(249,115,22,.3);}
              .h{background:linear-gradient(135deg,#431407,#7c2d12);padding:28px;text-align:center;}
              .logo{font-size:22px;font-weight:700;color:#fff;}
              .b{padding:28px;}
              h2{color:#fdba74;font-size:18px;margin:0 0 12px;}
              p{color:rgba(255,255,255,.65);font-size:14px;line-height:1.6;margin:0 0 12px;}
              .row{display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid rgba(255,255,255,.06);}
              .label{color:rgba(255,255,255,.4);font-size:12px;}
              .val{color:#fdba74;font-weight:600;}
              .f{border-top:1px solid rgba(255,255,255,.06);padding:16px 28px;font-size:11px;color:rgba(255,255,255,.2);text-align:center;}
            </style></head><body>
            <div class="c">
              <div class="h"><div class="logo">📦 CMMS — Estoque Baixo</div></div>
              <div class="b">
                <h2>Alerta de Reposição de Estoque</h2>
                <p>A peça abaixo está abaixo do nível mínimo configurado:</p>
                <div class="row"><span class="label">Peça</span><span class="val">%s</span></div>
                <div class="row"><span class="label">Código</span><span class="val">%s</span></div>
                <div class="row"><span class="label">Em estoque</span><span class="val">%d unid.</span></div>
                <div class="row"><span class="label">Mínimo</span><span class="val">%d unid.</span></div>
              </div>
              <div class="f">CMMS Industrial Suite · Alerta automático</div>
            </div></body></html>
            """.formatted(pecaNome, codigo, qtdAtual, qtdMinima);
        sendHtml(toEmail, "📦 Estoque Baixo — " + pecaNome, html);
    }

    public void sendBoasVindas(String toEmail, String nome, String nomeEmpresa) {
        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:#0d0d1a;}
              .c{max-width:540px;margin:40px auto;background:#12122a;border-radius:16px;border:1px solid rgba(139,92,246,.3);}
              .h{background:linear-gradient(135deg,#1e1b4b,#4c1d95);padding:28px;text-align:center;}
              .logo{font-size:22px;font-weight:700;color:#fff;}
              .b{padding:28px;}
              h2{color:#c4b5fd;font-size:18px;margin:0 0 12px;}
              p{color:rgba(255,255,255,.65);font-size:14px;line-height:1.6;margin:0 0 12px;}
              .btn{display:inline-block;background:#7c3aed;color:#fff;text-decoration:none;padding:11px 24px;border-radius:8px;font-weight:600;font-size:14px;margin-top:8px;}
              .f{border-top:1px solid rgba(255,255,255,.06);padding:16px 28px;font-size:11px;color:rgba(255,255,255,.2);text-align:center;}
            </style></head><body>
            <div class="c">
              <div class="h"><div class="logo">🎉 CMMS Industrial Suite</div></div>
              <div class="b">
                <h2>Bem-vindo(a), %s!</h2>
                <p>Sua empresa <strong style="color:#c4b5fd">%s</strong> foi criada com sucesso no CMMS Industrial Suite.</p>
                <p>Você está no plano <strong>Starter</strong> — pode cadastrar até 20 ativos e 3 usuários.</p>
                <p>Acesse o sistema e comece a gerenciar suas manutenções agora:</p>
                <a class="btn" href="%s">Acessar o Sistema</a>
              </div>
              <div class="f">CMMS Industrial Suite · Sistema de Manutenção Industrial</div>
            </div></body></html>
            """.formatted(nome != null ? nome : toEmail, nomeEmpresa, frontendUrl);
        sendHtml(toEmail, "🎉 Bem-vindo ao CMMS — " + nomeEmpresa, html);
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
