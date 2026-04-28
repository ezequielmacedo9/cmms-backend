package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.ManutencaoRequestDTO;
import br.com.cmms.cmms.dto.ManutencaoResponseDTO;
import br.com.cmms.cmms.model.Manutencao;
import br.com.cmms.cmms.model.Maquina;
import br.com.cmms.cmms.repository.ManutencaoRepository;
import br.com.cmms.cmms.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ManutencaoService {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoService.class);

    private final ManutencaoRepository manutencaoRepository;
    private final MaquinaRepository maquinaRepository;

    public ManutencaoService(ManutencaoRepository manutencaoRepository,
                             MaquinaRepository maquinaRepository) {
        this.manutencaoRepository = manutencaoRepository;
        this.maquinaRepository = maquinaRepository;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manutencoes", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public ManutencaoResponseDTO cadastrar(ManutencaoRequestDTO dto, Long maquinaId) {
        Maquina maquina = maquinaRepository.findById(maquinaId)
            .orElseThrow(() -> new RuntimeException("Máquina não encontrada: " + maquinaId));

        Manutencao m = new Manutencao();
        m.setTipo(dto.tipo());
        m.setTecnico(dto.tecnico());
        m.setDescricao(dto.descricao());
        m.setPrioridade(dto.prioridade() != null ? dto.prioridade() : "MEDIA");
        m.setStatus(dto.status() != null ? dto.status() : "ABERTA");
        m.setDataManutencao(dto.dataManutencao() != null ? dto.dataManutencao() : LocalDate.now());
        m.setPrazoSla(dto.prazoSla());
        m.setDataConclusao(dto.dataConclusao());
        m.setHorasParada(dto.horasParada());
        m.setCustoMaoDeObra(dto.custoMaoDeObra() != null ? dto.custoMaoDeObra() : 0.0);
        m.setObservacoesTecnico(dto.observacoesTecnico());
        m.setMaquina(maquina);

        if ("CONCLUIDA".equals(m.getStatus()) && m.getDataConclusao() == null) {
            m.setDataConclusao(LocalDate.now());
        }

        log.info("Registrando manutenção tipo={} para máquina id={}", dto.tipo(), maquinaId);
        return toDTO(manutencaoRepository.save(m));
    }

    @Cacheable("manutencoes")
    public List<ManutencaoResponseDTO> listar() {
        return manutencaoRepository.findAll().stream().map(this::toDTO).toList();
    }

    public Map<String, Object> listarPaginado(int page, int size, String tipo, String status) {
        List<ManutencaoResponseDTO> all = listar();
        List<ManutencaoResponseDTO> filtered = all.stream()
            .filter(m -> tipo == null || tipo.isBlank() || tipo.equalsIgnoreCase(m.tipo()))
            .filter(m -> status == null || status.isBlank() || status.equalsIgnoreCase(m.status()))
            .toList();

        int total = filtered.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);

        return Map.of(
            "content", filtered.subList(fromIdx, toIdx),
            "totalElements", total,
            "totalPages", (int) Math.ceil((double) total / size),
            "page", page,
            "size", size
        );
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manutencoes", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public ManutencaoResponseDTO atualizar(Long id, ManutencaoRequestDTO dto) {
        Manutencao m = manutencaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Manutenção não encontrada: " + id));

        m.setTipo(dto.tipo());
        m.setTecnico(dto.tecnico());
        m.setDescricao(dto.descricao());
        if (dto.prioridade() != null) m.setPrioridade(dto.prioridade());
        if (dto.status() != null) m.setStatus(dto.status());
        if (dto.dataManutencao() != null) m.setDataManutencao(dto.dataManutencao());
        if (dto.prazoSla() != null) m.setPrazoSla(dto.prazoSla());
        if (dto.dataConclusao() != null) m.setDataConclusao(dto.dataConclusao());
        if (dto.horasParada() != null) m.setHorasParada(dto.horasParada());
        if (dto.custoMaoDeObra() != null) m.setCustoMaoDeObra(dto.custoMaoDeObra());
        if (dto.observacoesTecnico() != null) m.setObservacoesTecnico(dto.observacoesTecnico());

        if ("CONCLUIDA".equals(dto.status()) && m.getDataConclusao() == null) {
            m.setDataConclusao(LocalDate.now());
            m.getMaquina().setDataUltimaManutencao(LocalDate.now());
            maquinaRepository.save(m.getMaquina());
        }

        log.info("Atualizando manutenção id={} status={}", id, m.getStatus());
        return toDTO(manutencaoRepository.save(m));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manutencoes", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public int gerarPreventivasVencidas() {
        LocalDate hoje = LocalDate.now();
        List<Maquina> vencidas = maquinaRepository.findByIntervaloPreventivaDiasGreaterThan(0).stream()
            .filter(m -> {
                if (m.getDataUltimaManutencao() == null) return true;
                return m.getDataUltimaManutencao().plusDays(m.getIntervaloPreventivaDias()).isBefore(hoje);
            })
            .toList();

        int geradas = 0;
        for (Maquina maquina : vencidas) {
            boolean jaAberta = manutencaoRepository
                .findByMaquinaIdOrderByDataManutencaoDesc(maquina.getId())
                .stream()
                .anyMatch(m -> "PREVENTIVA".equals(m.getTipo()) && "ABERTA".equals(m.getStatus()));

            if (!jaAberta) {
                Manutencao os = new Manutencao();
                os.setTipo("PREVENTIVA");
                os.setTecnico("A definir");
                os.setDescricao("Preventiva gerada automaticamente — intervalo: " + maquina.getIntervaloPreventivaDias() + " dias");
                os.setPrioridade("ALTA");
                os.setStatus("ABERTA");
                os.setDataManutencao(hoje);
                os.setPrazoSla(hoje.plusDays(3));
                os.setMaquina(maquina);
                manutencaoRepository.save(os);
                geradas++;
                log.info("OS preventiva gerada para máquina: {} ({})", maquina.getNome(), maquina.getId());
            }
        }
        return geradas;
    }

    public List<ManutencaoResponseDTO> listarPorMaquina(Long maquinaId) {
        return manutencaoRepository.findByMaquinaIdOrderByDataManutencaoDesc(maquinaId)
            .stream().map(this::toDTO).toList();
    }

    public ManutencaoResponseDTO buscarPorId(Long id) {
        return toDTO(manutencaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Manutenção não encontrada: " + id)));
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "manutencoes", allEntries = true),
        @CacheEvict(value = "dashboard-stats", allEntries = true)
    })
    public void deletar(Long id) {
        if (!manutencaoRepository.existsById(id)) {
            throw new RuntimeException("Manutenção não encontrada: " + id);
        }
        log.info("Deletando manutenção id={}", id);
        manutencaoRepository.deleteById(id);
    }

    public ManutencaoResponseDTO toDTO(Manutencao m) {
        ManutencaoResponseDTO.MaquinaInfo maquinaInfo = m.getMaquina() != null
            ? new ManutencaoResponseDTO.MaquinaInfo(m.getMaquina().getId(), m.getMaquina().getNome(), m.getMaquina().getSetor())
            : null;

        return new ManutencaoResponseDTO(
            m.getId(), m.getTipo(), m.getTecnico(), m.getDescricao(),
            m.getPrioridade() != null ? m.getPrioridade() : "MEDIA",
            m.getStatus() != null ? m.getStatus() : "ABERTA",
            m.getDataManutencao(),
            m.getPrazoSla(),
            m.getDataConclusao(),
            m.getHorasParada(),
            m.getCustoMaoDeObra(),
            m.calcularCustoTotal(),
            m.getObservacoesTecnico(),
            m.isSlaVencido(),
            maquinaInfo
        );
    }
}
