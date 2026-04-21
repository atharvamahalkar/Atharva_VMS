package com.example.rag.repository;

import com.example.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Integer> {
    List<DocumentChunk> findByContract_ContractId(String contractId);
}