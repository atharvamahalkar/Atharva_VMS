package com.example.rag.repository;

import com.example.rag.entity.ContractSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContractSectionRepository extends JpaRepository<ContractSection, Integer> {
    List<ContractSection> findByContract_ContractId(String contractId);
}