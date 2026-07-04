package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.ConfiguracaoSistema;
import br.com.cmms.cmms.repository.ConfiguracaoSistemaRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for the per-tenant {@link ConfiguracaoService}. */
@ExtendWith(MockitoExtension.class)
class ConfiguracaoServiceTest {

    @Mock ConfiguracaoSistemaRepository repo;
    @Mock TenantResolver tenant;
    @InjectMocks ConfiguracaoService service;

    @Test
    @DisplayName("seedForEmpresa: so insere as chaves que a empresa ainda nao tem")
    void seed_idempotente() {
        when(repo.existsByEmpresaIdAndChave(eq(1L), anyString())).thenReturn(true);
        when(repo.existsByEmpresaIdAndChave(1L, "empresa.nome")).thenReturn(false);

        service.seedForEmpresa(1L);

        verify(repo, times(1)).save(any(ConfiguracaoSistema.class));
    }

    @Test
    @DisplayName("salvar: atualiza chave existente da empresa e ignora chave desconhecida")
    void salvar_escopadoPorEmpresa() {
        when(tenant.requireEmpresaId()).thenReturn(7L);
        when(repo.existsByEmpresaIdAndChave(eq(7L), anyString())).thenReturn(true);
        ConfiguracaoSistema existente =
            new ConfiguracaoSistema(7L, "empresa.nome", "Antigo", "GERAL", "STRING", "Nome");
        when(repo.findByEmpresaIdAndChave(7L, "empresa.nome")).thenReturn(Optional.of(existente));
        when(repo.findByEmpresaIdAndChave(7L, "chave.inexistente")).thenReturn(Optional.empty());

        service.salvar(Map.of("empresa.nome", "Novo Nome", "chave.inexistente", "x"));

        assertThat(existente.getValor()).isEqualTo("Novo Nome");
        verify(repo, times(1)).save(existente);
    }

    @Test
    @DisplayName("getInt/getBool: leem por empresa e caem no default sem empresa")
    void getters_porEmpresa() {
        when(repo.findByEmpresaIdAndChave(1L, "seguranca.lockout.tentativas"))
            .thenReturn(Optional.of(new ConfiguracaoSistema(1L, "seguranca.lockout.tentativas", "3", "SEGURANCA", "NUMBER", "")));

        assertThat(service.getInt(1L, "seguranca.lockout.tentativas", 5)).isEqualTo(3);
        assertThat(service.getInt(null, "seguranca.lockout.tentativas", 5)).isEqualTo(5);
        assertThat(service.getBool(null, "seguranca.2fa.obrigatorio")).isFalse();
        verify(repo, never()).findByEmpresaIdAndChave(eq((Long) null), anyString());
        verify(repo, times(1)).findByEmpresaIdAndChave(anyLong(), anyString());
    }
}
