package com.example.rag.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_sections")
public class ContractSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sectionId;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(nullable = false)
    private String sectionName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sectionText;

    private Integer sectionOrder;
    private String clauseType;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Integer getSectionId() { return sectionId; }
    public void setSectionId(Integer sectionId) { this.sectionId = sectionId; }

    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getSectionText() { return sectionText; }
    public void setSectionText(String sectionText) { this.sectionText = sectionText; }

    public Integer getSectionOrder() { return sectionOrder; }
    public void setSectionOrder(Integer sectionOrder) { this.sectionOrder = sectionOrder; }

    public String getClauseType() { return clauseType; }
    public void setClauseType(String clauseType) { this.clauseType = clauseType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}