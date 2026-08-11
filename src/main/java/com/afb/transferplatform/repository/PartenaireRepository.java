package com.afb.transferplatform.repository;

import com.afb.transferplatform.entity.Partenaire;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartenaireRepository extends JpaRepository<Partenaire, Long> {
    boolean existsByNomIgnoreCase(String nom);
}
