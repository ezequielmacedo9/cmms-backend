package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock ManutencaoRepository manutencaoRepo;
    @Mock MaquinaRepository maquinaRepo;
    @Mock PecaRepository pecaRepo;
    @Mock TenantResolver tenant;
    @InjectMocks RelatorioService service;

    @Test
    @DisplayName("maquinasPdf: muitas linhas geram MULTIPLAS paginas (sem truncar)")
    void maquinasPdf_paginatesAcrossPages() throws Exception {
        when(tenant.requireEmpresaId()).thenReturn(1L);

        List<Maquina> muitas = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            Maquina m = new Maquina("Maquina " + i, "Setor " + (i % 5), "ATIVO");
            m.setPrioridade("MEDIA");
            muitas.add(m);
        }
        when(maquinaRepo.findByEmpresaId(1L)).thenReturn(muitas);

        byte[] pdf = service.maquinasPdf();

        assertThat(pdf).isNotEmpty();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    @Test
    @DisplayName("manutencoesPdf/estoquePdf: geram PDF valido de 1 pagina com dados")
    void pdfs_geramDocumentoValido() throws Exception {
        when(tenant.requireEmpresaId()).thenReturn(1L);
        when(manutencaoRepo.findByEmpresaIdOrderByDataManutencaoDesc(1L)).thenReturn(List.of(sampleManutencao()));
        when(pecaRepo.findByEmpresaId(1L)).thenReturn(List.of(samplePeca()));

        for (byte[] pdf : List.of(service.manutencoesPdf(), service.estoquePdf())) {
            assertThat(pdf).isNotEmpty();
            try (PDDocument doc = Loader.loadPDF(pdf)) {
                assertThat(doc.getNumberOfPages()).isEqualTo(1);
            }
        }
    }

    @Test
    @DisplayName("excel: manutencoes/maquinas/estoque geram planilha com header + 1 linha")
    void excels_geramPlanilhaComDados() throws Exception {
        when(tenant.requireEmpresaId()).thenReturn(1L);
        when(manutencaoRepo.findByEmpresaIdOrderByDataManutencaoDesc(1L)).thenReturn(List.of(sampleManutencao()));
        when(maquinaRepo.findByEmpresaId(1L)).thenReturn(List.of(sampleMaquina()));
        when(pecaRepo.findByEmpresaId(1L)).thenReturn(List.of(samplePeca()));

        for (byte[] xlsx : List.of(service.manutencoesExcel(), service.maquinasExcel(), service.estoqueExcel())) {
            assertThat(xlsx).isNotEmpty();
            try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
                assertThat(wb.getNumberOfSheets()).isEqualTo(1);
                // header row + exactly one data row
                assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(1);
            }
        }
    }

    // ── fixtures ─────────────────────────────────────────────────────────

    private static Maquina sampleMaquina() {
        Maquina m = new Maquina("Compressor 01", "Utilidades", "ATIVO");
        ReflectionTestUtils.setField(m, "id", 1L);
        m.setPrioridade("ALTA");
        return m;
    }

    private static Manutencao sampleManutencao() {
        Manutencao m = new Manutencao();
        ReflectionTestUtils.setField(m, "id", 10L);
        m.setMaquina(sampleMaquina());
        m.setTipo("CORRETIVA");
        m.setTecnico("Ana");
        m.setDescricao("Troca de rolamento");
        m.setPrioridade("ALTA");
        m.setStatus("CONCLUIDA");
        m.setDataManutencao(LocalDate.now());
        return m;
    }

    private static Peca samplePeca() {
        Peca p = new Peca();
        ReflectionTestUtils.setField(p, "id", 5L);
        p.setNome("Rolamento 6204");
        p.setCodigo("ROL-6204");
        p.setQuantidadeEmEstoque(12);
        p.setVidaUtilHoras(8000);
        p.setCustoUnitario(29.90);
        return p;
    }
}
