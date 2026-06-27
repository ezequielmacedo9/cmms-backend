package br.com.cmms.cmms.service;

import br.com.cmms.cmms.exception.ForbiddenException;
import br.com.cmms.cmms.model.Assinatura;
import br.com.cmms.cmms.repository.AssinaturaRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssinaturaServiceTest {

    @Mock AssinaturaRepository assinaturaRepo;
    @Mock MaquinaRepository maquinaRepo;
    @Mock UsuarioRepository usuarioRepo;
    @Mock PagamentoService pagamentoService;
    @InjectMocks AssinaturaService service;

    private static Assinatura comPlano(String plano, String status) {
        Assinatura a = new Assinatura(1L);
        a.setPlano(plano);
        a.setStatus(status);
        return a;
    }

    @Test
    @DisplayName("quota máquina: lança QUOTA_ATIVOS quando atinge o limite do plano STARTER")
    void quotaMaquina_atLimit_throws() {
        when(assinaturaRepo.findByEmpresaId(1L)).thenReturn(Optional.of(comPlano("STARTER", "TRIAL")));
        when(maquinaRepo.countByEmpresaId(1L)).thenReturn(15L); // STARTER = 15

        assertThatThrownBy(() -> service.assertPodeCriarMaquina(1L))
            .isInstanceOf(ForbiddenException.class)
            .extracting("errorCode").isEqualTo("QUOTA_ATIVOS");
    }

    @Test
    @DisplayName("quota máquina: permite quando abaixo do limite")
    void quotaMaquina_belowLimit_ok() {
        when(assinaturaRepo.findByEmpresaId(1L)).thenReturn(Optional.of(comPlano("STARTER", "TRIAL")));
        when(maquinaRepo.countByEmpresaId(1L)).thenReturn(14L);

        assertThatCode(() -> service.assertPodeCriarMaquina(1L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("quota máquina: ENTERPRISE é ilimitado (nem conta)")
    void quotaMaquina_enterpriseUnlimited() {
        when(assinaturaRepo.findByEmpresaId(1L)).thenReturn(Optional.of(comPlano("ENTERPRISE", "ATIVA")));

        assertThatCode(() -> service.assertPodeCriarMaquina(1L)).doesNotThrowAnyException();
        verify(maquinaRepo, never()).countByEmpresaId(anyLong());
    }

    @Test
    @DisplayName("checkout: ativa o plano escolhido e devolve link de pagamento vazio")
    void checkout_activatesPlan() {
        Assinatura a = comPlano("STARTER", "TRIAL");
        when(assinaturaRepo.findByEmpresaId(1L)).thenReturn(Optional.of(a));
        when(assinaturaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Gateway desligado -> link vazio -> ativacao manual.
        when(pagamentoService.criarLinkPagamento(anyLong(), any())).thenReturn("");

        var res = service.checkout(1L, "PRO");

        assertThat(res.assinatura().plano()).isEqualTo("PRO");
        assertThat(res.assinatura().status()).isEqualTo("ATIVA");
        assertThat(res.linkPagamento()).isEmpty();
    }

    @Test
    @DisplayName("confirmarPagamento: ativa a assinatura da empresa")
    void confirmarPagamento_ativa() {
        Assinatura a = comPlano("PRO", "TRIAL");
        when(assinaturaRepo.findByEmpresaId(1L)).thenReturn(Optional.of(a));
        when(assinaturaRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.confirmarPagamento(1L);

        assertThat(a.getStatus()).isEqualTo("ATIVA");
    }

    @Test
    @DisplayName("planos: catálogo expõe os 4 tiers")
    void planos_catalog() {
        assertThat(service.planos()).containsKeys("STARTER", "PRO", "BUSINESS", "ENTERPRISE");
    }
}
