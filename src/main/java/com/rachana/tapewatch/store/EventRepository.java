package com.rachana.tapewatch.store;
import com.rachana.tapewatch.domain.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventRepository extends JpaRepository<EventEntity,String> {}
