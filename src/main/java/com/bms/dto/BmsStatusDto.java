package com.bms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BmsStatusDto {
    private Double totalVoltage; // 총 전압 (V)
    private Double current; // 전류 (A)
    private Double temperature; // 배터리 온도 (°C)
    private Double remainingCapacity; // 잔여용량 (%)
    private Boolean chargeFetStatus; // 충전 FET 상태
    private Boolean dischargeFetStatus; // 방전 FET 상태
    private List<Double> cellVoltages; // 각 셀의 전압 (V)
    private String timestamp;
}
