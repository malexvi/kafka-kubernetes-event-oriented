package com.sfr.sfr_orquestrador_api.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "package_dimension")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageDimension {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private double height;
    private double width;
    private double length;
    private double weight;
}
