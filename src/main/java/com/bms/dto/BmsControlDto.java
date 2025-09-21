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

    // BMS 임계값 설정
    private Double overchargeVoltage; // 과충전 임계값 (V)
    private Double underchargeVoltage; // 과방전 임계값 (V)
    private Double overchargeCurrent; // 과충전 전류값 (A)
    private Double dischargeCurrent; // 과방전 전류값 (A)
    
    // 딜레이 시간 설정
    private Integer voltageDelay; // 전압 딜레이 시간 (초)
    private Integer chargeCurrentDelay; // 충전 전류 딜레이 시간 (초)
    private Integer chargeCurrentRelease; // 충전 전류 해제 시간 (초)
    private Integer dischargeCurrentDelay; // 방전 전류 딜레이 시간 (초)
    private Integer dischargeCurrentRelease; // 방전 전류 해제 시간 (초)
    
    // 제어 명령 타입
    private String commandType; // set_OV, set_UV, set_ChgOC, set_DsgOC, set_delayVoltage, set_delayChgOC, set_delayDsgOC, Reset_settings

}
