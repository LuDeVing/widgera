package com.widgera.repository;

import com.widgera.entity.PromptHistory;
import com.widgera.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {

    List<PromptHistory> findByUserOrderByCreatedAtDesc(User user);

    Page<PromptHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

}
