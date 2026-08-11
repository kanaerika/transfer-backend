package com.afb.transferplatform.repository;

import com.afb.transferplatform.entity.Agent;
import com.afb.transferplatform.entity.CompteurJournalier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CompteurJournalierRepository extends JpaRepository<CompteurJournalier, Long> {
    Optional<CompteurJournalier> findByAgentAndJour(Agent agent, LocalDate jour);
}