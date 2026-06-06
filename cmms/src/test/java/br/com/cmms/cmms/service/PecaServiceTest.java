package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.PecaRequestDTO;
import br.com.cmms.cmms.dto.PecaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Peca;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock PecaRepository pecaRepository;
    @Mock TenantResolver tenant;
    @InjectMocks PecaService pecaService;

    private Peca peca;
    private PecaRequestDTO sampleDto;

    @BeforeEach
    void setUp() {
        lenient().when(tenant.requireEmpresaId()).thenReturn(1L);
        peca = new Peca();
        ReflectionTestUtils.setField(peca, "id", 5L);
        peca.setNome("Rolamento 6204");
        peca.setCodigo("ROL-6204");
        peca.setQuantidadeEmEstoque(50);
        peca.setCustoUnitario(29.90);
        peca.setVidaUtilHoras(8000);

        sampleDto = new PecaRequestDTO();
        sampleDto.setNome("Rolamento 6204");
        sampleDto.setCodigo("ROL-6204");
        sampleDto.setQuantidadeEmEstoque(50);
        sampleDto.setCustoUnitario(29.90);
        sampleDto.setVidaUtilHoras(8000);
    }

    @Test
    @DisplayName("cadastrar: copia DTO -> entity e mapeia de volta")
    void cadastrar() {
        when(pecaRepository.save(any(Peca.class))).thenAnswer(i -> {
            Peca p = i.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 1L);
            return p;
        });

        PecaResponseDTO out = pecaService.cadastrar(sampleDto);

        assertThat(out.getNome()).isEqualTo("Rolamento 6204");
        assertThat(out.getCodigo()).isEqualTo("ROL-6204");
        assertThat(out.getQuantidadeEmEstoque()).isEqualTo(50);
        assertThat(out.getCustoUnitario()).isEqualTo(29.90);
    }

    @Test
    @DisplayName("listar(q, Pageable): normaliza string vazia para null")
    void listar_paged_normalizes() {
        Pageable p = PageRequest.of(0, 20);
        when(pecaRepository.search(eq(null), eq(1L), eq(p)))
            .thenReturn(new PageImpl<>(List.of(peca), p, 1));

        Page<PecaResponseDTO> result = pecaService.listar("  ", p);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(pecaRepository).search(null, 1L, p);
    }

    @Test
    @DisplayName("buscarPorId: 404 quando ausente")
    void buscarPorId_notFound() {
        when(pecaRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pecaService.buscarPorId(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("atualizar: aplica DTO e salva")
    void atualizar_happy() {
        when(pecaRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(peca));
        when(pecaRepository.save(any(Peca.class))).thenAnswer(i -> i.getArgument(0));

        sampleDto.setQuantidadeEmEstoque(100);
        PecaResponseDTO out = pecaService.atualizar(5L, sampleDto);

        assertThat(out.getQuantidadeEmEstoque()).isEqualTo(100);
    }

    @Test
    @DisplayName("atualizar: 404 quando id nao existe")
    void atualizar_notFound() {
        when(pecaRepository.findByIdAndEmpresaId(404L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pecaService.atualizar(404L, sampleDto))
            .isInstanceOf(NotFoundException.class);
        verify(pecaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletar: HARD delete (Peca nao tem soft delete), escopado por empresa")
    void deletar_hardDelete() {
        when(pecaRepository.findByIdAndEmpresaId(5L, 1L)).thenReturn(Optional.of(peca));
        pecaService.deletar(5L);
        verify(pecaRepository).delete(peca);
    }

    @Test
    @DisplayName("deletar: 404 quando ausente")
    void deletar_notFound() {
        when(pecaRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pecaService.deletar(99L))
            .isInstanceOf(NotFoundException.class);
    }
}
