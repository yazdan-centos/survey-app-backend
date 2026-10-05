package org.mapnaom.surveyappbackend.repository;

import org.mapnaom.surveyappbackend.entity.TableCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableCatalogEntryRepository extends JpaRepository<TableCatalogEntry, Long> {
    Optional<TableCatalogEntry> findByTableName(String tableName);
}
