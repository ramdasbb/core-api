package com.smartvillage.noticeboard.repository;

import com.smartvillage.noticeboard.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {
    List<Notice> findByIsActiveTrueOrderByNoticeDateDesc();
}
