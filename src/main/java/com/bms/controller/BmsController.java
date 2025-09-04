package com.bms.controller;

import com.bms.dto.BmsControlDto;
import com.bms.dto.BmsStatusDto;
import com.bms.model.BmsData;
import com.bms.service.BmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/bms")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BmsController {

    private final BmsService bmsService;

    @GetMapping("/status")
    public ResponseEntity<BmsStatusDto> getLatestStatus() {
        BmsStatusDto status = bmsService.getLatestBmsStatus();
        if (status != null) {
            return ResponseEntity.ok(status);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<BmsData>> getHistory(
            @RequestParam String start,
            @RequestParam String end) {
        try {
            // ISO 8601 형식 (예: 2025-08-13T06:27:01.010Z)을 파싱
            LocalDateTime startTime = LocalDateTime.parse(start.replace("Z", ""));
            LocalDateTime endTime = LocalDateTime.parse(end.replace("Z", ""));
            
            List<BmsData> history = bmsService.getBmsHistory(startTime, endTime);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error parsing date parameters", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/control")
    public ResponseEntity<String> sendControlCommand(@RequestBody BmsControlDto controlDto) {
        try {
            bmsService.sendControlCommand(controlDto);
            return ResponseEntity.ok("Control command sent successfully");
        } catch (Exception e) {
            log.error("Error sending control command", e);
            return ResponseEntity.internalServerError().body("Failed to send control command");
        }
    }

    @PostMapping("/control/charge")
    public ResponseEntity<String> controlChargeFet(@RequestParam boolean status) {
        try {
            BmsControlDto controlDto = new BmsControlDto();
            controlDto.setChargeFetStatus(status);
            controlDto.setDischargeFetStatus(null); // 기존 상태 유지
            
            bmsService.sendControlCommand(controlDto);
            return ResponseEntity.ok("Charge FET control command sent: " + (status ? "ON" : "OFF"));
        } catch (Exception e) {
            log.error("Error controlling charge FET", e);
            return ResponseEntity.internalServerError().body("Failed to control charge FET");
        }
    }

    @PostMapping("/control/discharge")
    public ResponseEntity<String> controlDischargeFet(@RequestParam boolean status) {
        try {
            BmsControlDto controlDto = new BmsControlDto();
            controlDto.setDischargeFetStatus(status);
            controlDto.setChargeFetStatus(null); // 기존 상태 유지
            
            bmsService.sendControlCommand(controlDto);
            return ResponseEntity.ok("Discharge FET control command sent: " + (status ? "ON" : "OFF"));
        } catch (Exception e) {
            log.error("Error controlling discharge FET", e);
            return ResponseEntity.internalServerError().body("Failed to control discharge FET");
        }
    }

    @GetMapping("/temperature/history")
    public ResponseEntity<List<BmsData>> getTemperatureHistory(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<BmsData> temperatureHistory = bmsService.getTemperatureHistory(limit);
            return ResponseEntity.ok(temperatureHistory);
        } catch (Exception e) {
            log.error("Error getting temperature history", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/control/electronic-load")
    public ResponseEntity<String> controlElectronicLoad(@RequestBody BmsControlDto controlDto) {
        try {
            bmsService.sendControlCommand(controlDto);
            String mode = controlDto.getLoadMode();
            String level = controlDto.getCpModeLevel() != null ? " (단계: " + controlDto.getCpModeLevel() + ")" : "";
            String status = controlDto.getElectronicLoadEnabled() ? "ON" : "OFF";
            return ResponseEntity.ok("Electronic load control command sent: " + status + " Mode: " + mode + level);
        } catch (Exception e) {
            log.error("Error controlling electronic load", e);
            return ResponseEntity.internalServerError().body("Failed to control electronic load");
        }
    }

    @PostMapping("/control/charge-discharge")
    public ResponseEntity<String> controlChargeDischarge(@RequestBody BmsControlDto controlDto) {
        try {
            bmsService.sendControlCommand(controlDto);
            String chargeStatus = controlDto.getChargeEnabled() != null ? 
                (controlDto.getChargeEnabled() ? "충전 ON" : "충전 OFF") : "";
            String dischargeStatus = controlDto.getDischargeEnabled() != null ? 
                (controlDto.getDischargeEnabled() ? "방전 ON" : "방전 OFF") : "";
            return ResponseEntity.ok("Charge/Discharge control command sent: " + chargeStatus + " " + dischargeStatus);
        } catch (Exception e) {
            log.error("Error controlling charge/discharge", e);
            return ResponseEntity.internalServerError().body("Failed to control charge/discharge");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("BMS Control System is running");
    }
}
