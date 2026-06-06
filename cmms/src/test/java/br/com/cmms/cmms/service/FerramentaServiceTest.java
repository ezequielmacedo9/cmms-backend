package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.FerramentaRequestDTO;
import br.com.cmms.cmms.dto.FerramentaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Ferramenta;
import br.com.cmms.cmms.repository.FerramentaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FerramentaServiceTest {

    @Mock FerramentaRepository ferramentaRepository;
    @Mock TenantResolver tenant;
    @InjectMocks FerramentaService ferramentaService;

    private Ferramenta ferramenta;

    @BeforeEach
    void setUp() {
        lenient().when(tenant.requireEmpresaId()).thenReturn(1L);
        ferramenta = new Ferramenta();
        ReflectionTestUtils.setField(ferramenta, "id", 3L);
        ferramenta.setNome("Torquímetro 1/2''");
        ferramenta.setCodigo("TQ-1-2");
        ferramenta.setStatus("DISPONIVEL");
        ferramenta.setLocalizacao("Almoxarifado A3");
        ferramenta.setResponsavel("Ana");
        ferramenta.setDataUltimaManutencao(LocalDate.now().minusMonths(2));
    }

    @Test
    @DisplayName("cadastrar: aplica DTO completo")
    void cadastrar() {
        var dto = new FerramentaRequestDTO("Nova", "NOV-1", "DISPONIVEL",
                                            "Bancada 1", "Bob",
                                            LocalDate.now().minusDays(1));
        when(ferramentaRepository.save(any(Ferramenta.class))).thenAnswer(i -> {
            Ferramenta f = i.getArgument(0);
            ReflectionTestUtils.setField(f, "id", 1L);
            return f;
        });

        FerramentaResponseDTO out = ferramentaService.cadastrar(dto);

        assertThat(out.nome()).isEqualTo("Nova");
        assertThat(out.codigo()).isEqualTo("NOV-1");
        assertThat(out.localizacao()).isEqualTo("Bancada 1");
        assertThat(out.responsavel()).isEqualTo("Bob");
    }

    @Test
    @DisplayName("listar(q, Pageable): normaliza string em branco para null")
    void listar_paged_normalizes() {
        var p = PageRequest.of(0, 20);
        when(ferramentaRepository.search(eq(null), eq(1L), eq(p)))
            .thenReturn(new PageImpl<>(List.of(ferramenta), p, 1));

        var page = ferramentaService.listar("   ", p);

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(ferramentaRepository).search(null, 1L, p);
    }

    @Test
    @DisplayName("buscarPorId: 404 quando ausente")
    void buscarPorId_notFound() {
        when(ferramentaRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ferramentaService.buscarPorId(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("atualizar: aplica DTO e salva")
    void atualizar_happy() {
        when(ferramentaRepository.findByIdAndEmpresaId(3L, 1L)).thenReturn(Optional.of(ferramenta));
        when(ferramentaRepository.save(any(Ferramenta.class))).thenAnswer(i -> i.getArgument(0));

        var dto = new FerramentaRequestDTO("Renomeada", "NEW-CODE", "EM_USO",
                                            "Setor B", "Carlos", LocalDate.now());

        FerramentaResponseDTO out = ferramentaService.atualizar(3L, dto);

        assertThat(out.nome()).isEqualTo("Renomeada");
        assertThat(out.status()).isEqualTo("EM_USO");
        assertThat(out.responsavel()).isEqualTo("Carlos");
    }

    @Test
    @DisplayName("atualizar: 404 quando id nao existe")
    void atualizar_notFound() {
        when(ferramentaRepository.findByIdAndEmpresaId(404L, 1L)).thenReturn(Optional.empty());
        var dto = new FerramentaRequestDTO("X", null, null, null, null, null);

        assertThatThrownBy(() -> ferramentaService.atualizar(404L, dto))
            .isInstanceOf(NotFoundException.class);
        verify(ferramentaRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletar: HARD delete (Ferramenta nao tem soft delete), escopado por empresa")
    void deletar_happy() {
        when(ferramentaRepository.findByIdAndEmpresaId(3L, 1L)).thenReturn(Optional.of(ferramenta));
        ferramentaService.deletar(3L);
        verify(ferramentaRepository).delete(ferramenta);
    }

    @Test
    @DisplayName("deletar: 404 quando ausente")
    void deletar_notFound() {
        when(ferramentaRepository.findByIdAndEmpresaId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ferramentaService.deletar(99L))
            .isInstanceOf(NotFoundException.class);
        verify(ferramentaRepository, never()).delete(any());
    }
}
