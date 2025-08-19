package com.bms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BmsControlDto {
    @JsonProperty("charge_fet_status")
    private Boolean chargeFetStatus; // 충전 FET 상태 (true: ON, false: OFF)
    
    @JsonProperty("discharge_fet_status")
    private Boolean dischargeFetStatus; // 방전 FET 상태 (true: ON, false: OFF)
}
