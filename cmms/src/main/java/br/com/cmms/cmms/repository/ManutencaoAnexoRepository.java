package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.ManutencaoAnexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ManutencaoAnexoRepository extends JpaRepository<ManutencaoAnexo, Long> {

    Optional<ManutencaoAnexo> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Metadata only (no base64 blob) for listing in the work-order detail. Returns {@code [id, nome, contentType, tamanho]}. */
    @Query("SELECT a.id, a.nome, a.contentType, a.tamanho FROM ManutencaoAnexo a WHERE a.manutencaoId = :manutencaoId ORDER BY a.id")
    List<Object[]> findMetaByManutencaoId(@Param("manutencaoId") Long manutencaoId);
}
