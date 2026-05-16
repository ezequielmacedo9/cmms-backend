package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RelatorioService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ManutencaoRepository manutencaoRepo;
    private final MaquinaRepository maquinaRepo;
    private final PecaRepository pecaRepo;

    public RelatorioService(ManutencaoRepository manutencaoRepo,
                            MaquinaRepository maquinaRepo,
                            PecaRepository pecaRepo) {
        this.manutencaoRepo = manutencaoRepo;
        this.maquinaRepo = maquinaRepo;
        this.pecaRepo = pecaRepo;
    }

    // ── EXCEL ─────────────────────────────────────────────────────────────

    public byte[] manutencoesExcel() throws Exception {
        List<Manutencao> lista = manutencaoRepo.findAll();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Manutenções");
            CellStyle hs = headerStyle(wb);
            String[] cols = {"ID","Máquina","Setor","Tipo","Prioridade","Status","Técnico","Descrição","Data"};
            writeHeader(sheet, hs, cols);
            sheet.setColumnWidth(0, 8*256); sheet.setColumnWidth(1, 22*256);
            sheet.setColumnWidth(2, 18*256); sheet.setColumnWidth(7, 35*256); sheet.setColumnWidth(8, 14*256);
            int row = 1;
            for (Manutencao m : lista) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(m.getId());
                r.createCell(1).setCellValue(m.getMaquina() != null ? m.getMaquina().getNome() : "");
                r.createCell(2).setCellValue(m.getMaquina() != null ? m.getMaquina().getSetor() : "");
                r.createCell(3).setCellValue(nvl(m.getTipo()));
                r.createCell(4).setCellValue(nvl(m.getPrioridade()));
                r.createCell(5).setCellValue(nvl(m.getStatus()));
                r.createCell(6).setCellValue(nvl(m.getTecnico()));
                r.createCell(7).setCellValue(nvl(m.getDescricao()));
                r.createCell(8).setCellValue(m.getDataManutencao() != null ? m.getDataManutencao().format(FMT) : "");
            }
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, cols.length - 1));
            return toBytes(wb);
        }
    }

    public byte[] maquinasExcel() throws Exception {
        List<Maquina> lista = maquinaRepo.findAll();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Máquinas");
            CellStyle hs = headerStyle(wb);
            String[] cols = {"ID","Nome","Setor","Status","Prioridade","Última Manut.","Intervalo Prev. (dias)"};
            writeHeader(sheet, hs, cols);
            for (int i = 0; i < cols.length; i++) sheet.setColumnWidth(i, 20*256);
            int row = 1;
            for (Maquina m : lista) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(m.getId());
                r.createCell(1).setCellValue(nvl(m.getNome()));
                r.createCell(2).setCellValue(nvl(m.getSetor()));
                r.createCell(3).setCellValue(nvl(m.getStatus()));
                r.createCell(4).setCellValue(nvl(m.getPrioridade()));
                r.createCell(5).setCellValue(m.getDataUltimaManutencao() != null ? m.getDataUltimaManutencao().toString() : "");
                Integer intervalo = m.getIntervaloPreventivaDias();
                r.createCell(6).setCellValue(intervalo != null ? intervalo : 0);
            }
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, cols.length - 1));
            return toBytes(wb);
        }
    }

    public byte[] estoqueExcel() throws Exception {
        List<Peca> lista = pecaRepo.findAll();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Estoque");
            CellStyle hs = headerStyle(wb);
            String[] cols = {"ID","Nome","Código","Qtd. Estoque","Vida Útil (h)","Custo Unit. (R$)"};
            writeHeader(sheet, hs, cols);
            for (int i = 0; i < cols.length; i++) sheet.setColumnWidth(i, 20*256);
            int row = 1;
            for (Peca p : lista) {
                Row r = sheet.createRow(row++);
                r.createCell(0).setCellValue(p.getId());
                r.createCell(1).setCellValue(nvl(p.getNome()));
                r.createCell(2).setCellValue(nvl(p.getCodigo()));
                r.createCell(3).setCellValue(p.getQuantidadeEmEstoque());
                r.createCell(4).setCellValue(p.getVidaUtilHoras());
                r.createCell(5).setCellValue(p.getCustoUnitario());
            }
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, cols.length - 1));
            return toBytes(wb);
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────────

    public byte[] manutencoesPdf() throws Exception {
        List<Manutencao> lista = manutencaoRepo.findAll();
        return buildPdf("Relatório de Manutenções", lista.size(),
            new String[]{"Máquina","Tipo","Prioridade","Status","Data"},
            new float[]{0.30f,0.15f,0.15f,0.15f,0.25f},
            lista.stream().map(m -> new String[]{
                m.getMaquina() != null ? m.getMaquina().getNome() : "-",
                nvl(m.getTipo()), nvl(m.getPrioridade()), nvl(m.getStatus()),
                m.getDataManutencao() != null ? m.getDataManutencao().format(FMT) : "-"
            }).toList());
    }

    public byte[] maquinasPdf() throws Exception {
        List<Maquina> lista = maquinaRepo.findAll();
        return buildPdf("Relatório de Máquinas", lista.size(),
            new String[]{"Nome","Setor","Status","Prioridade","Últ. Manut."},
            new float[]{0.28f,0.18f,0.14f,0.14f,0.26f},
            lista.stream().map(m -> new String[]{
                nvl(m.getNome()), nvl(m.getSetor()), nvl(m.getStatus()),
                nvl(m.getPrioridade()),
                m.getDataUltimaManutencao() != null ? m.getDataUltimaManutencao().format(FMT) : "-"
            }).toList());
    }

    public byte[] estoquePdf() throws Exception {
        List<Peca> lista = pecaRepo.findAll();
        return buildPdf("Relatório de Estoque", lista.size(),
            new String[]{"Nome","Código","Qtd.","Vida Útil (h)","Custo (R$)"},
            new float[]{0.35f,0.2f,0.1f,0.15f,0.2f},
            lista.stream().map(p -> new String[]{
                nvl(p.getNome()), nvl(p.getCodigo()),
                String.valueOf(p.getQuantidadeEmEstoque()),
                String.valueOf(p.getVidaUtilHoras()),
                String.format("%.2f", p.getCustoUnitario())
            }).toList());
    }

    // ── PDF builder ───────────────────────────────────────────────────────

    private byte[] buildPdf(String title, int total, String[] headers, float[] ratios,
                            java.util.List<String[]> rows) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            float margin = 40f;
            addPage(doc, title, total, headers, ratios, rows, margin);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            return bos.toByteArray();
        }
    }

    private void addPage(PDDocument doc, String title, int total, String[] headers,
                         float[] ratios, java.util.List<String[]> rows, float margin) throws Exception {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float pageH = page.getMediaBox().getHeight();
        float pageW = page.getMediaBox().getWidth() - 2 * margin;
        float y = pageH - margin;

        PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDColor purple  = rgb(0.49f, 0.23f, 0.93f);
        PDColor white   = rgb(1f, 1f, 1f);
        PDColor grey    = rgb(0.55f, 0.55f, 0.55f);
        PDColor dark    = rgb(0.15f, 0.08f, 0.28f);
        PDColor rowGrey = rgb(0.92f, 0.90f, 0.95f);
        PDColor rowWhite= rgb(1f, 1f, 1f);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Title bar
            cs.setNonStrokingColor(dark);
            cs.addRect(0, pageH - 60, page.getMediaBox().getWidth(), 60);
            cs.fill();
            cs.beginText();
            cs.setFont(bold, 16);
            cs.setNonStrokingColor(white);
            cs.newLineAtOffset(margin, pageH - 38);
            cs.showText(title);
            cs.endText();
            cs.beginText();
            cs.setFont(regular, 9);
            cs.setNonStrokingColor(rgb(0.7f,0.6f,1f));
            cs.newLineAtOffset(margin, pageH - 52);
            cs.showText("CMMS Industrial Suite  •  Gerado em " + LocalDate.now().format(FMT) + "  •  Total: " + total);
            cs.endText();
            y = pageH - 72;

            // Column headers
            float rowH = 16f;
            cs.setNonStrokingColor(purple);
            cs.addRect(margin, y - rowH + 4, pageW, rowH);
            cs.fill();
            cs.beginText();
            cs.setFont(bold, 8);
            cs.setNonStrokingColor(white);
            float cx = margin + 4;
            cs.newLineAtOffset(cx, y - rowH + 8);
            for (int i = 0; i < headers.length; i++) {
                if (i > 0) { cs.newLineAtOffset(pageW * ratios[i-1], 0); cx += pageW * ratios[i-1]; }
                cs.showText(headers[i]);
            }
            cs.endText();
            y -= rowH + 2;

            // Data rows
            boolean alt = false;
            for (String[] row : rows) {
                if (y < margin + 20) break;
                cs.setNonStrokingColor(alt ? rowGrey : rowWhite);
                cs.addRect(margin, y - rowH + 4, pageW, rowH);
                cs.fill();
                cs.beginText();
                cs.setFont(regular, 8);
                cs.setNonStrokingColor(rgb(0.1f,0.05f,0.2f));
                float rx = margin + 4;
                cs.newLineAtOffset(rx, y - rowH + 8);
                for (int i = 0; i < row.length && i < headers.length; i++) {
                    if (i > 0) { cs.newLineAtOffset(pageW * ratios[i-1], 0); }
                    String v = row[i]; if (v.length() > 28) v = v.substring(0, 26) + "…";
                    cs.showText(v);
                }
                cs.endText();
                y -= rowH;
                alt = !alt;
            }

            // Footer
            cs.setNonStrokingColor(grey);
            cs.beginText();
            cs.setFont(regular, 7);
            cs.newLineAtOffset(margin, margin - 10);
            cs.showText("CMMS Industrial Suite — Documento gerado automaticamente");
            cs.endText();
        }
    }

    // ── utils ─────────────────────────────────────────────────────────────

    private static PDColor rgb(float r, float g, float b) {
        return new PDColor(new float[]{r, g, b}, PDDeviceRGB.INSTANCE);
    }

    private static String nvl(String s) { return s != null ? s : ""; }

    private static void writeHeader(Sheet sheet, CellStyle style, String[] cols) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < cols.length; i++) {
            Cell c = header.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(style);
        }
    }

    private static CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(f);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        return bos.toByteArray();
    }
}
