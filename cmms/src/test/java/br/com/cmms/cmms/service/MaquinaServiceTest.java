package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.MaquinaRequestDTO;
import br.com.cmms.cmms.dto.MaquinaResponseDTO;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.MaquinaRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MaquinaService}.
 *
 * <p>Cobre:
 * <ul>
 *   <li>cadastrar — happy + defaults aplicados quando DTO omite</li>
 *   <li>listar(q, status, Pageable) — passa nulos quando vazio</li>
 *   <li>buscarPorId — 404 quando ausente</li>
 *   <li>atualizar — 404 quando ausente; happy aplica DTO</li>
 *   <li>deletar — soft delete (deleted_at preenchido em save)</li>
 *   <li>isVencida — regra de preventiva no toDTO</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MaquinaServiceTest {

    @Mock MaquinaRepository maquinaRepository;
    @InjectMocks MaquinaService maquinaService;

    private Maquina maquina;

    @BeforeEach
    void setUp() {
        maquina = new Maquina();
        ReflectionTestUtils.setField(maquina, "id", 7L);
        maquina.setNome("Torno CNC 02");
        maquina.setSetor("Usinagem");
        maquina.setStatus("ATIVO");
        maquina.setPrioridade("MEDIA");
        maquina.setIntervaloPreventivaDias(30);
        maquina.setDataUltimaManutencao(LocalDate.now().minusDays(5));
    }

    @Test
    @DisplayName("cadastrar: salva e aplica prioridade default MEDIA quando ausente")
    void cadastrar_appliesDefaults() {
        when(maquinaRepository.save(any(Maquina.class))).thenAnswer(i -> {
            Maquina m = i.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 1L);
            return m;
        });

        var dto = new MaquinaRequestDTO("Nova", "Setor X", "ATIVO",
                                       null, null, null);

        MaquinaResponseDTO out = maquinaService.cadastrar(dto);

        assertThat(out.nome()).isEqualTo("Nova");
        assertThat(out.prioridade()).isEqualTo("MEDIA");
        assertThat(out.intervaloPreventivaDias()).isZero();
        assertThat(out.manutencaoVencida()).isFalse();
    }

    @Test
    @DisplayName("listar(q, status, Pageable): normaliza strings vazias para null e mapeia Page")
    void listar_normalizesAndMaps() {
        Pageable p = PageRequest.of(0, 10);
        when(maquinaRepository.search(eq(null), eq(null), eq(p)))
            .thenReturn(new PageImpl<>(List.of(maquina), p, 1));

        Page<MaquinaResponseDTO> page = maquinaService.listar("  ", "", p);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).nome()).isEqualTo("Torno CNC 02");
        verify(maquinaRepository).search(null, null, p);
    }

    @Test
    @DisplayName("listar(q, status, Pageable): mantem strings nao-vazias")
    void listar_passesNonEmptyFilters() {
        Pageable p = PageRequest.of(0, 10);
        when(maquinaRepository.search(eq("torno"), eq("ATIVO"), eq(p)))
            .thenReturn(new PageImpl<>(List.of(maquina), p, 1));

        maquinaService.listar("torno", "ATIVO", p);
        verify(maquinaRepository).search("torno", "ATIVO", p);
    }

    @Test
    @DisplayName("buscarPorId: lanca NotFoundException com code MAQUINA_NOT_FOUND")
    void buscarPorId_notFound() {
        when(maquinaRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maquinaService.buscarPorId(42L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("buscarPorId: happy retorna DTO completo")
    void buscarPorId_happy() {
        when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina));
        MaquinaResponseDTO out = maquinaService.buscarPorId(7L);
        assertThat(out.id()).isEqualTo(7L);
        assertThat(out.setor()).isEqualTo("Usinagem");
    }

    @Test
    @DisplayName("atualizar: 404 quando id nao existe")
    void atualizar_notFound() {
        when(maquinaRepository.findById(42L)).thenReturn(Optional.empty());
        var dto = new MaquinaRequestDTO("X", "Y", "ATIVO", "ALTA", 10, null);

        assertThatThrownBy(() -> maquinaService.atualizar(42L, dto))
            .isInstanceOf(NotFoundException.class);
        verify(maquinaRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar: aplica DTO completo e salva")
    void atualizar_happy() {
        when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina));
        when(maquinaRepository.save(any(Maquina.class))).thenAnswer(i -> i.getArgument(0));

        var dto = new MaquinaRequestDTO("Renomeada", "Outro", "INATIVO",
                                       "CRITICA", 60, LocalDate.now());

        MaquinaResponseDTO out = maquinaService.atualizar(7L, dto);

        assertThat(out.nome()).isEqualTo("Renomeada");
        assertThat(out.setor()).isEqualTo("Outro");
        assertThat(out.status()).isEqualTo("INATIVO");
        assertThat(out.prioridade()).isEqualTo("CRITICA");
        assertThat(out.intervaloPreventivaDias()).isEqualTo(60);
    }

    @Test
    @DisplayName("deletar: SOFT delete — set deletedAt + save (nao chama deleteById)")
    void deletar_softDelete() {
        when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina));

        maquinaService.deletar(7L);

        ArgumentCaptor<Maquina> captor = ArgumentCaptor.forClass(Maquina.class);
        verify(maquinaRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        assertThat(captor.getValue().isDeleted()).isTrue();
        verify(maquinaRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("deletar: 404 quando id nao existe")
    void deletar_notFound() {
        when(maquinaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> maquinaService.deletar(99L))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("isVencida: false quando interval=0 ou null")
    void isVencida_zeroOrNull() {
        maquina.setIntervaloPreventivaDias(0);
        MaquinaResponseDTO out = maquinaService.toDTO(maquina);
        assertThat(out.manutencaoVencida()).isFalse();
    }

    @Test
    @DisplayName("isVencida: true quando dataUltima=null e interval>0")
    void isVencida_nullDate() {
        maquina.setIntervaloPreventivaDias(30);
        maquina.setDataUltimaManutencao(null);
        MaquinaResponseDTO out = maquinaService.toDTO(maquina);
        assertThat(out.manutencaoVencida()).isTrue();
    }

    @Test
    @DisplayName("isVencida: true quando data + interval < hoje")
    void isVencida_overdue() {
        maquina.setIntervaloPreventivaDias(10);
        maquina.setDataUltimaManutencao(LocalDate.now().minusDays(20)); // 20 > 10
        MaquinaResponseDTO out = maquinaService.toDTO(maquina);
        assertThat(out.manutencaoVencida()).isTrue();
    }

    @Test
    @DisplayName("isVencida: false quando ainda dentro do prazo")
    void isVencida_within() {
        maquina.setIntervaloPreventivaDias(30);
        maquina.setDataUltimaManutencao(LocalDate.now().minusDays(5));
        MaquinaResponseDTO out = maquinaService.toDTO(maquina);
        assertThat(out.manutencaoVencida()).isFalse();
    }
}
