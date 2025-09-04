package com.bms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BmsControlDto {
    private Boolean chargeFetStatus; // 충전 FET 상태 (true: ON, false: OFF)
    private Boolean dischargeFetStatus; // 방전 FET 상태 (true: ON, false: OFF)
    
    // 전자부하 제어
    private Boolean electronicLoadEnabled; // 전자부하 활성화 (true: ON, false: OFF)
    private String loadMode; // CC: 정전류, CP: 정전력
    private Integer cpModeLevel; // CP 모드일 때 1~5단계
    
    // 충방전 제어
    private Boolean chargeEnabled; // 충전 ON/OFF
    private Boolean dischargeEnabled; // 방전 ON/OFF
}
