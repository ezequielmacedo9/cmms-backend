package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.ManutencaoChecklistItem;
import br.com.cmms.cmms.model.ManutencaoPeca;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.model.Peca;
import br.com.cmms.cmms.repository.ManutencaoAnexoRepository;
import br.com.cmms.cmms.repository.ManutencaoChecklistRepository;
import br.com.cmms.cmms.repository.ManutencaoPecaRepository;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.PecaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManutencaoServiceTest {

    @Mock ManutencaoRepository manutencaoRepository;
    @Mock MaquinaRepository    maquinaRepository;
    @Mock PecaRepository       pecaRepository;
    @Mock ManutencaoPecaRepository manutencaoPecaRepository;
    @Mock ManutencaoChecklistRepository checklistRepository;
    @Mock ManutencaoAnexoRepository anexoRepository;
    @Mock TenantResolver       tenant;
    @InjectMocks ManutencaoService manutencaoService;

    private Maquina maquina;
    private Manutencao manutencao;

    @BeforeEach
    void setUp() {
        lenient().when(tenant.requireEmpresaId()).thenReturn(1L);
        lenient().when(manutencaoPecaRepository.findByManutencaoId(anyLong())).thenReturn(List.of());
        lenient().when(checklistRepository.findByManutencaoIdOrderById(anyLong())).thenReturn(List.of());
        lenient().when(anexoRepository.findMetaByManutencaoId(anyLong())).thenReturn(List.of());
        maquina = new Maquina();
        ReflectionTestUtils.setField(maquina, "id", 1L);
        maquina.setNome("Torno");
        maquina.setSetor("Usinagem");

        manutencao = new Manutencao();
        ReflectionTestUtils.setField(manutencao, "id", 10L);
        manutencao.setMaquina(maquina);
        manutencao.setTipo("PREVENTIVA");
        manutencao.setTecnico("Ana");
        manutencao.setDescricao("Troca de filtro");
        manutencao.setPrioridade("MEDIA");
        manutencao.setStatus("ABERTA");
        manutencao.setDataManutencao(LocalDate.now());
    }

    @Test
    @DisplayName("cadastrar: 404 quando maquina nao existe")
    void cadastrar_maquinaNotFound() {
        when(maquinaRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());
        var dto = new ManutencaoRequestDTO("PREVENTIVA", "Ana", null, "x", null, null, null, null, null, null);

        assertThatThrownBy(() -> manutencaoService.cadastrar(dto, 99L))
            .isInstanceOf(NotFoundException.class);
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cadastrar: defaults aplicados quando DTO omite prioridade/status/data")
    void cadastrar_appliesDefaults() {
        when(maquinaRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(maquina));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(i -> {
            Manutencao m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 100L);
            return m;
        });

        var dto = new ManutencaoRequestDTO("CORRETIVA", "Bob", null, "Desc",
                                          null, null, null, null, null, null);

        ManutencaoResponseDTO out = manutencaoService.cadastrar(dto, 1L);

        assertThat(out.tipo()).isEqualTo("CORRETIVA");
        assertThat(out.prioridade()).isEqualTo("MEDIA");
        assertThat(out.status()).isEqualTo("ABERTA");
        assertThat(out.dataManutencao()).isEqualTo(LocalDate.now());
        assertThat(out.maquina().nome()).isEqualTo("Torno");
    }

    @Test
    @DisplayName("listar(Pageable): mapeia Page<Manutencao> para Page<DTO>")
    void listar_paged() {
        var p = PageRequest.of(0, 5);
        when(manutencaoRepository.findByEmpresaId(1L, p))
            .thenReturn(new PageImpl<>(List.of(manutencao), p, 1));

        var page = manutencaoService.listar(p);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).tipo()).isEqualTo("PREVENTIVA");
        assertThat(page.getContent().get(0).maquina().setor()).isEqualTo("Usinagem");
    }

    @Test
    @DisplayName("buscarPorId: 404 quando ausente")
    void buscarPorId_notFound() {
        when(manutencaoRepository.findByIdAndEmpresaId(404L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> manutencaoService.buscarPorId(404L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("deletar: SOFT delete (deleted_at preenchido + save)")
    void deletar_softDelete() {
        when(manutencaoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(manutencao));

        manutencaoService.deletar(10L);

        ArgumentCaptor<Manutencao> captor = ArgumentCaptor.forClass(Manutencao.class);
        verify(manutencaoRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().isDeleted()).isTrue();
        verify(manutencaoRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deletar: 404 quando ausente, sem tocar no save")
    void deletar_notFound() {
        when(manutencaoRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> manutencaoService.deletar(99L))
            .isInstanceOf(NotFoundException.class);
        verify(manutencaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("toDTO: maquina ausente vira MaquinaInfo null")
    void toDTO_nullMaquina() {
        manutencao.setMaquina(null);
        ManutencaoResponseDTO out = manutencaoService.toDTO(manutencao);
        assertThat(out.maquina()).isNull();
    }

    @Test
    @DisplayName("cadastrar com pecas: da baixa no estoque e registra consumo")
    void cadastrar_comPecas_dabaixaNoEstoque() {
        when(maquinaRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(maquina));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(i -> {
            Manutencao m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 55L);
            return m;
        });
        Peca peca = new Peca();
        ReflectionTestUtils.setField(peca, "id", 9L);
        peca.setNome("Rolamento");
        peca.setQuantidadeEmEstoque(10);
        peca.setCustoUnitario(20.0);
        when(pecaRepository.findByIdAndEmpresaId(9L, 1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(any(Peca.class))).thenAnswer(i -> i.getArgument(0));

        var dto = new ManutencaoRequestDTO("CORRETIVA", "Bob", null, "x", null, null, null, null, null,
            List.of(new ManutencaoRequestDTO.PecaConsumo(9L, 3)));
        manutencaoService.cadastrar(dto, 1L);

        assertThat(peca.getQuantidadeEmEstoque()).isEqualTo(7);
        verify(manutencaoPecaRepository).save(any(ManutencaoPeca.class));
    }

    @Test
    @DisplayName("cadastrar com estoque insuficiente: lanca ESTOQUE_INSUFICIENTE")
    void cadastrar_estoqueInsuficiente_lancaValidation() {
        when(maquinaRepository.findByIdAndEmpresaId(1L, 1L)).thenReturn(Optional.of(maquina));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(i -> {
            Manutencao m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 55L);
            return m;
        });
        Peca peca = new Peca();
        ReflectionTestUtils.setField(peca, "id", 9L);
        peca.setNome("Rolamento");
        peca.setQuantidadeEmEstoque(2);
        when(pecaRepository.findByIdAndEmpresaId(9L, 1L)).thenReturn(Optional.of(peca));

        var dto = new ManutencaoRequestDTO("CORRETIVA", "Bob", null, "x", null, null, null, null, null,
            List.of(new ManutencaoRequestDTO.PecaConsumo(9L, 5)));

        assertThatThrownBy(() -> manutencaoService.cadastrar(dto, 1L))
            .isInstanceOf(ValidationException.class)
            .extracting("errorCode").isEqualTo("ESTOQUE_INSUFICIENTE");
    }

    @Test
    @DisplayName("alterarStatus CONCLUIDA: carimba dataConclusao")
    void alterarStatus_concluida_setaDataConclusao() {
        when(manutencaoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(manutencao));
        when(manutencaoRepository.save(any(Manutencao.class))).thenAnswer(i -> i.getArgument(0));

        ManutencaoResponseDTO out = manutencaoService.alterarStatus(10L, "CONCLUIDA");

        assertThat(out.status()).isEqualTo("CONCLUIDA");
        assertThat(out.dataConclusao()).isNotNull();
    }

    @Test
    @DisplayName("alterarStatus invalido: lanca INVALID_STATUS")
    void alterarStatus_invalido() {
        assertThatThrownBy(() -> manutencaoService.alterarStatus(10L, "XPTO"))
            .isInstanceOf(ValidationException.class)
            .extracting("errorCode").isEqualTo("INVALID_STATUS");
    }

    @Test
    @DisplayName("addChecklistItem: salva o item na OS da empresa")
    void addChecklistItem_salva() {
        when(manutencaoRepository.findByIdAndEmpresaId(10L, 1L)).thenReturn(Optional.of(manutencao));

        manutencaoService.addChecklistItem(10L, "Trocar filtro");

        verify(checklistRepository).save(any(ManutencaoChecklistItem.class));
    }

    @Test
    @DisplayName("addAnexo: base64 acima do limite lanca ANEXO_MUITO_GRANDE")
    void addAnexo_muitoGrande() {
        String grande = "a".repeat(4_000_001);
        assertThatThrownBy(() -> manutencaoService.addAnexo(10L, "foto.png", "image/png", grande))
            .isInstanceOf(ValidationException.class)
            .extracting("errorCode").isEqualTo("ANEXO_MUITO_GRANDE");
    }
}
