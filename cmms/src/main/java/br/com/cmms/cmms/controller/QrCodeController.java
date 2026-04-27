package br.com.cmms.cmms.controller;

import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/maquinas")
public class QrCodeController {

    private final MaquinaRepository repo;

    @Value("${app.frontend.url:https://cmms-frontend.vercel.app}")
    private String frontendUrl;

    public QrCodeController(MaquinaRepository repo) { this.repo = repo; }

    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> qrcode(@PathVariable Long id,
                                          @RequestParam(defaultValue = "200") int size) {
        Maquina m = repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Máquina não encontrada"));

        String content = frontendUrl + "/maquinas?id=" + id +
                         "&nome=" + encUrl(m.getNome()) +
                         "&setor=" + encUrl(m.getSetor());
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN, 2
            );
            int px = Math.max(100, Math.min(size, 600));
            var matrix = writer.encode(content, BarcodeFormat.QR_CODE, px, px, hints);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", bos);

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"maquina-" + id + "-qr.png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("QR code generation failed", e);
        }
    }

    private String encUrl(String s) {
        if (s == null) return "";
        return s.replace(" ", "%20").replace("&", "%26");
    }
}
