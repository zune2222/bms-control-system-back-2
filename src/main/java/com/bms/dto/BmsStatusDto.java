package com.bms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BmsStatusDto {
    @JsonProperty("total_voltage")
    private Double totalVoltage; // 총 전압 (V)
    
    private Double current; // 전류 (A)
    
    private Double temperature; // 배터리 온도 (°C)
    
    @JsonProperty("remaining_capacity_percent")
    private Double remainingCapacity; // 잔여용량 (%)
    
    @JsonProperty("charge_fet_status")
    private Boolean chargeFetStatus; // 충전 FET 상태 (true: ON, false: OFF, null: 알 수 없음)
    
    @JsonProperty("discharge_fet_status")
    private Boolean dischargeFetStatus; // 방전 FET 상태 (true: ON, false: OFF, null: 알 수 없음)
    
    @JsonProperty("cell_voltages")
    private List<Double> cellVoltages; // 각 셀의 전압 (V)
    
    private String timestamp;
}
