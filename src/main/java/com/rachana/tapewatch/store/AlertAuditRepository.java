package com.rachana.tapewatch.store;
import com.rachana.tapewatch.domain.AlertAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AlertAuditRepository extends JpaRepository<AlertAuditEntity,Long>{List<AlertAuditEntity> findAllByAlertIdOrderByTimestampAsc(Long alertId);}
