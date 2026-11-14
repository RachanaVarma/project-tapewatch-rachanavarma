package com.rachana.tapewatch.store;
import com.rachana.tapewatch.domain.AlertEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface AlertRepository extends JpaRepository<AlertEntity,Long>, JpaSpecificationExecutor<AlertEntity> {
    Page<AlertEntity> findAllByOrderByEventTimeDesc(Pageable pageable);
}
