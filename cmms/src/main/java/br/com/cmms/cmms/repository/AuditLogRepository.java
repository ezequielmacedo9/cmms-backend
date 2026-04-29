package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<AuditLog> findByEmpresaIdOrderByTimestampDesc(Long empresaId, Pageable pageable);
    Page<AuditLog> findByUserEmailContainingIgnoreCaseOrderByTimestampDesc(String email, Pageable pageable);
    Page<AuditLog> findByEmpresaIdAndUserEmailContainingIgnoreCaseOrderByTimestampDesc(Long empresaId, String email, Pageable pageable);
}
