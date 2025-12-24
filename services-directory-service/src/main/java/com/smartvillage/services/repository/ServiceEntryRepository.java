package com.smartvillage.services.repository;

import com.smartvillage.services.entity.ServiceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceEntryRepository extends JpaRepository<ServiceEntry, UUID> {
}
