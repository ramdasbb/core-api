package com.smartvillage.devwork.repository;

import com.smartvillage.devwork.entity.DevWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DevWorkRepository extends JpaRepository<DevWork, UUID> {
}
