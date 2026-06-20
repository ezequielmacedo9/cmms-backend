package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.Maquina;
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

import java.util.ArrayList;
import java.util.List;

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
}
