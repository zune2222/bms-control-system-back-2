package com.bms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bms_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BmsData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "total_voltage")
    private Double totalVoltage; // 총 전압 (V)
    
    @Column(name = "current")
    private Double current; // 전류 (A)
    
    @Column(name = "temperature")
    private Double temperature; // 배터리 온도 (°C)
    
    @Column(name = "remaining_capacity")
    private Double remainingCapacity; // 잔여용량 (%)
    
    @Column(name = "charge_fet_status")
    private Boolean chargeFetStatus; // 충전 FET 상태 (true: ON, false: OFF)
    
    @Column(name = "discharge_fet_status")
    private Boolean dischargeFetStatus; // 방전 FET 상태 (true: ON, false: OFF)
    
    @ElementCollection
    @CollectionTable(name = "cell_voltages", joinColumns = @JoinColumn(name = "bms_data_id"))
    @Column(name = "voltage")
    private List<Double> cellVoltages; // 각 셀의 전압 (V)
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
