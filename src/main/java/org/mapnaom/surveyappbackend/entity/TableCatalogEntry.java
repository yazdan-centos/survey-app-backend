package org.mapnaom.surveyappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_table_catalog")
public class TableCatalogEntry extends BaseEntity {
    @Column(name = "table_name", nullable = false, unique = true, length = 150)
    private String tableName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(name = "is_system", nullable = false)
    private boolean system;
}
