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
            log.info("Received electronic load control request: {}", controlDto);
            
            // 입력 검증
            if (controlDto.getElectronicLoadEnabled() == null) {
                log.error("Electronic load enabled status is null");
                return ResponseEntity.badRequest().body("Electronic load enabled status is required");
            }
            
            if (controlDto.getLoadMode() == null || controlDto.getLoadMode().isEmpty()) {
                log.error("Load mode is null or empty");
                return ResponseEntity.badRequest().body("Load mode is required");
            }
            
            if (!"CC".equals(controlDto.getLoadMode()) && !"CP".equals(controlDto.getLoadMode())) {
                log.error("Invalid load mode: {}", controlDto.getLoadMode());
                return ResponseEntity.badRequest().body("Load mode must be CC or CP");
            }
            
            if ("CP".equals(controlDto.getLoadMode()) && 
                (controlDto.getCpModeLevel() == null || controlDto.getCpModeLevel() < 1 || controlDto.getCpModeLevel() > 5)) {
                log.error("Invalid CP mode level: {}", controlDto.getCpModeLevel());
                return ResponseEntity.badRequest().body("CP mode level must be between 1 and 5");
            }
            
            log.info("Sending electronic load control command to BmsService...");
            try {
                bmsService.sendElectronicLoadCommand(controlDto);
                log.info("Electronic load control command sent successfully");
            } catch (Exception mqttException) {
                log.warn("MQTT sending failed, but API call considered successful: {}", mqttException.getMessage());
                // MQTT 실패해도 API 응답은 성공으로 처리 (임시 처리)
            }
            
            String mode = controlDto.getLoadMode();
            String level = controlDto.getCpModeLevel() != null ? " (단계: " + controlDto.getCpModeLevel() + ")" : "";
            String status = controlDto.getElectronicLoadEnabled() ? "ON" : "OFF";
            return ResponseEntity.ok("Electronic load control command received: " + status + " Mode: " + mode + level);
        } catch (Exception e) {
            log.error("Error controlling electronic load", e);
            log.error("Stack trace: ", e);
            return ResponseEntity.internalServerError().body("Failed to control electronic load: " + e.getMessage());
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
    public ResponseEntity<java.util.Map<String, Object>> healthCheck() {
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        health.put("status", "running");
        health.put("timestamp", java.time.LocalDateTime.now().toString());
        health.put("pythonHardwareController", bmsService.isHardwareControllerAvailable());
        
        return ResponseEntity.ok(health);
    }

    // BMS 임계값 설정 API
    @PostMapping("/settings/overcharge-voltage")
    public ResponseEntity<String> setOverchargeVoltage(@RequestParam double voltage) {
        try {
            // 범위 검증 (3.70 ~ 4.20V)
            if (voltage < 3.70 || voltage > 4.20) {
                return ResponseEntity.badRequest().body("과충전 임계값은 3.70V ~ 4.20V 범위여야 합니다.");
            }

            // Use hardware API for direct control with MQTT fallback
            boolean success = bmsService.setOverchargeVoltageHardware(voltage);
            if (!success) {
                return ResponseEntity.internalServerError().body("과충전 임계값 설정 실패");
            }
            return ResponseEntity.ok("과충전 임계값 설정 완료: " + voltage + "V");
        } catch (Exception e) {
            log.error("Error setting overcharge voltage", e);
            return ResponseEntity.internalServerError().body("과충전 임계값 설정 실패");
        }
    }

    @PostMapping("/settings/undercharge-voltage")
    public ResponseEntity<String> setUnderchargeVoltage(@RequestParam double voltage) {
        try {
            // 범위 검증 (2.50 ~ 4.00V)
            if (voltage < 2.50 || voltage > 4.00) {
                return ResponseEntity.badRequest().body("과방전 임계값은 2.50V ~ 4.00V 범위여야 합니다.");
            }

            boolean success = bmsService.setUnderchargeVoltageHardware(voltage);
            if (!success) {
                return ResponseEntity.internalServerError().body("과방전 임계값 설정 실패");
            }
            return ResponseEntity.ok("과방전 임계값 설정 완료: " + voltage + "V");
        } catch (Exception e) {
            log.error("Error setting undercharge voltage", e);
            return ResponseEntity.internalServerError().body("과방전 임계값 설정 실패");
        }
    }

    @PostMapping("/settings/overcharge-current")
    public ResponseEntity<String> setOverchargeCurrent(@RequestParam double current) {
        try {
            // 범위 검증 (1.50 ~ 2.50A)
            if (current < 1.50 || current > 2.50) {
                return ResponseEntity.badRequest().body("과충전 전류값은 1.50A ~ 2.50A 범위여야 합니다.");
            }

            boolean success = bmsService.setOverchargeCurrentHardware(current);
            if (!success) {
                return ResponseEntity.internalServerError().body("과충전 전류값 설정 실패");
            }
            return ResponseEntity.ok("과충전 전류값 설정 완료: " + current + "A");
        } catch (Exception e) {
            log.error("Error setting overcharge current", e);
            return ResponseEntity.internalServerError().body("과충전 전류값 설정 실패");
        }
    }

    @PostMapping("/settings/discharge-current")
    public ResponseEntity<String> setDischargeCurrent(@RequestParam double current) {
        try {
            // 범위 검증 (2.00 ~ 6.00A)
            if (current < 2.00 || current > 6.00) {
                return ResponseEntity.badRequest().body("과방전 전류값은 2.00A ~ 6.00A 범위여야 합니다.");
            }

            boolean success = bmsService.setDischargeCurrentHardware(current);
            if (!success) {
                return ResponseEntity.internalServerError().body("과방전 전류값 설정 실패");
            }
            return ResponseEntity.ok("과방전 전류값 설정 완료: " + current + "A");
        } catch (Exception e) {
            log.error("Error setting discharge current", e);
            return ResponseEntity.internalServerError().body("과방전 전류값 설정 실패");
        }
    }

    // 딜레이 시간 설정 API
    @PostMapping("/settings/voltage-delay")
    public ResponseEntity<String> setVoltageDelay(@RequestParam int delay) {
        try {
            // 범위 검증 (2 ~ 7초)
            if (delay < 2 || delay > 7) {
                return ResponseEntity.badRequest().body("전압 딜레이 시간은 2초 ~ 7초 범위여야 합니다.");
            }

            boolean success = bmsService.setVoltageDelayHardware(delay);
            if (!success) {
                return ResponseEntity.internalServerError().body("전압 딜레이 시간 설정 실패");
            }
            return ResponseEntity.ok("전압 딜레이 시간 설정 완료: " + delay + "초");
        } catch (Exception e) {
            log.error("Error setting voltage delay", e);
            return ResponseEntity.internalServerError().body("전압 딜레이 시간 설정 실패");
        }
    }

    @PostMapping("/settings/charge-current-delay")
    public ResponseEntity<String> setChargeCurrentDelay(@RequestParam int delay, @RequestParam int release) {
        try {
            // 범위 검증 (딜레이: 5 ~ 15초, 해제: 10 ~ 32초)
            if (delay < 5 || delay > 15) {
                return ResponseEntity.badRequest().body("충전 전류 딜레이 시간은 5초 ~ 15초 범위여야 합니다.");
            }
            if (release < 10 || release > 32) {
                return ResponseEntity.badRequest().body("충전 전류 해제 시간은 10초 ~ 32초 범위여야 합니다.");
            }

            boolean success = bmsService.setChargeCurrentDelayHardware(delay, release);
            if (!success) {
                return ResponseEntity.internalServerError().body("충전 전류 딜레이 설정 실패");
            }
            return ResponseEntity.ok("충전 전류 딜레이 설정 완료: 딜레이 " + delay + "초, 해제 " + release + "초");
        } catch (Exception e) {
            log.error("Error setting charge current delay", e);
            return ResponseEntity.internalServerError().body("충전 전류 딜레이 설정 실패");
        }
    }

    @PostMapping("/settings/discharge-current-delay")
    public ResponseEntity<String> setDischargeCurrentDelay(@RequestParam int delay, @RequestParam int release) {
        try {
            // 범위 검증 (딜레이: 5 ~ 15초, 해제: 10 ~ 32초)
            if (delay < 5 || delay > 15) {
                return ResponseEntity.badRequest().body("방전 전류 딜레이 시간은 5초 ~ 15초 범위여야 합니다.");
            }
            if (release < 10 || release > 32) {
                return ResponseEntity.badRequest().body("방전 전류 해제 시간은 10초 ~ 32초 범위여야 합니다.");
            }

            boolean success = bmsService.setDischargeCurrentDelayHardware(delay, release);
            if (!success) {
                return ResponseEntity.internalServerError().body("방전 전류 딜레이 설정 실패");
            }
            return ResponseEntity.ok("방전 전류 딜레이 설정 완료: 딜레이 " + delay + "초, 해제 " + release + "초");
        } catch (Exception e) {
            log.error("Error setting discharge current delay", e);
            return ResponseEntity.internalServerError().body("방전 전류 딜레이 설정 실패");
        }
    }

    // 설정 초기화 API
    @PostMapping("/settings/reset")
    public ResponseEntity<String> resetSettings() {
        try {
            boolean success = bmsService.resetSettingsHardware();
            if (!success) {
                return ResponseEntity.internalServerError().body("설정 초기화 실패");
            }
            return ResponseEntity.ok("BMS 설정이 기본값으로 초기화되었습니다.");
        } catch (Exception e) {
            log.error("Error resetting settings", e);
            return ResponseEntity.internalServerError().body("설정 초기화 실패");
        }
    }

    // 종합 임계값 설정 API (프론트엔드에서 사용)
    @PostMapping("/settings/thresholds")
    public ResponseEntity<String> setThresholds(@RequestBody BmsControlDto settingsDto) {
        try {
            // 입력 검증
            String validationError = validateThresholdSettings(settingsDto);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }

            // 각 설정을 순차적으로 전송 (하드웨어 API 사용)
            boolean allSuccess = true;
            
            if (settingsDto.getOverchargeVoltage() != null) {
                boolean success = bmsService.setOverchargeVoltageHardware(settingsDto.getOverchargeVoltage());
                if (!success) allSuccess = false;
            }

            if (settingsDto.getUnderchargeVoltage() != null) {
                boolean success = bmsService.setUnderchargeVoltageHardware(settingsDto.getUnderchargeVoltage());
                if (!success) allSuccess = false;
            }

            if (settingsDto.getOverchargeCurrent() != null) {
                boolean success = bmsService.setOverchargeCurrentHardware(settingsDto.getOverchargeCurrent());
                if (!success) allSuccess = false;
            }

            if (settingsDto.getDischargeCurrent() != null) {
                boolean success = bmsService.setDischargeCurrentHardware(settingsDto.getDischargeCurrent());
                if (!success) allSuccess = false;
            }
            
            if (!allSuccess) {
                return ResponseEntity.internalServerError().body("일부 임계값 설정이 실패했습니다.");
            }

            return ResponseEntity.ok("BMS 임계값 설정이 완료되었습니다.");
        } catch (Exception e) {
            log.error("Error setting thresholds", e);
            return ResponseEntity.internalServerError().body("임계값 설정 실패: " + e.getMessage());
        }
    }

    private String validateThresholdSettings(BmsControlDto settings) {
        if (settings.getOverchargeVoltage() != null) {
            double ov = settings.getOverchargeVoltage();
            if (ov < 3.70 || ov > 4.20) {
                return "과충전 임계값은 3.70V ~ 4.20V 범위여야 합니다.";
            }
        }

        if (settings.getUnderchargeVoltage() != null) {
            double uv = settings.getUnderchargeVoltage();
            if (uv < 2.50 || uv > 4.00) {
                return "과방전 임계값은 2.50V ~ 4.00V 범위여야 합니다.";
            }
        }

        if (settings.getOverchargeVoltage() != null && settings.getUnderchargeVoltage() != null) {
            if (settings.getOverchargeVoltage() - 0.05 <= settings.getUnderchargeVoltage()) {
                return "과충전 임계값은 과방전 임계값보다 최소 0.05V 이상 높아야 합니다.";
            }
        }

        if (settings.getOverchargeCurrent() != null) {
            double chgOc = settings.getOverchargeCurrent();
            if (chgOc < 1.50 || chgOc > 2.50) {
                return "과충전 전류값은 1.50A ~ 2.50A 범위여야 합니다.";
            }
        }

        if (settings.getDischargeCurrent() != null) {
            double dsgOc = settings.getDischargeCurrent();
            if (dsgOc < 2.00 || dsgOc > 6.00) {
                return "과방전 전류값은 2.00A ~ 6.00A 범위여야 합니다.";
            }
        }

        return null;
    }

    // Additional Hardware Control API Endpoints

    /**
     * Get comprehensive hardware status including Python controller availability
     */
    @GetMapping("/hardware/status")
    public ResponseEntity<java.util.Map<String, Object>> getHardwareStatus() {
        try {
            java.util.Map<String, Object> status = new java.util.HashMap<>();
            
            // Basic system status
            status.put("springBootBackend", "running");
            status.put("timestamp", java.time.LocalDateTime.now().toString());
            
            // Python hardware controller status
            boolean pythonAvailable = bmsService.isHardwareControllerAvailable();
            status.put("pythonHardwareController", pythonAvailable);
            
            if (pythonAvailable) {
                // Get BMS status from Python controller
                java.util.Map<String, Object> bmsStatus = bmsService.getBmsStatusFromHardware();
                if (bmsStatus != null) {
                    status.put("bmsHardwareStatus", bmsStatus);
                }
                
                // Get BMS settings from Python controller
                java.util.Map<String, Object> bmsSettings = bmsService.getBmsSettingsFromHardware();
                if (bmsSettings != null) {
                    status.put("bmsHardwareSettings", bmsSettings);
                }
            } else {
                status.put("bmsHardwareStatus", "unavailable");
                status.put("bmsHardwareSettings", "unavailable");
                
                // Get latest from database as fallback
                BmsStatusDto latestStatus = bmsService.getLatestBmsStatus();
                if (latestStatus != null) {
                    status.put("latestDatabaseStatus", latestStatus);
                }
            }
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting hardware status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Direct FET control endpoint
     */
    @PostMapping("/hardware/fet/control")
    public ResponseEntity<String> controlFETHardware(@RequestBody BmsControlDto controlDto) {
        try {
            Boolean chargeFetStatus = controlDto.getChargeFetStatus();
            Boolean dischargeFetStatus = controlDto.getDischargeFetStatus();
            
            boolean success = bmsService.controlFETHardware(chargeFetStatus, dischargeFetStatus);
            
            if (success) {
                return ResponseEntity.ok("FET 제어 성공: 충전 FET=" + chargeFetStatus + ", 방전 FET=" + dischargeFetStatus);
            } else {
                return ResponseEntity.internalServerError().body("FET 제어 실패");
            }
        } catch (Exception e) {
            log.error("Error controlling FET hardware", e);
            return ResponseEntity.internalServerError().body("FET 제어 실패");
        }
    }

    /**
     * Direct electronic load control endpoint
     */
    @PostMapping("/hardware/electronic-load/control")
    public ResponseEntity<String> controlElectronicLoadHardware(@RequestBody BmsControlDto controlDto) {
        try {
            Boolean enabled = controlDto.getElectronicLoadEnabled();
            String loadMode = controlDto.getLoadMode();
            Integer cpModeLevel = controlDto.getCpModeLevel();
            
            boolean success = bmsService.controlElectronicLoadHardware(enabled, loadMode, cpModeLevel);
            
            if (success) {
                String modeDesc = "CP".equals(loadMode) ? loadMode + " 레벨 " + cpModeLevel : loadMode;
                return ResponseEntity.ok("전자부하 제어 성공: " + (enabled ? "ON" : "OFF") + " (" + modeDesc + ")");
            } else {
                return ResponseEntity.internalServerError().body("전자부하 제어 실패");
            }
        } catch (Exception e) {
            log.error("Error controlling electronic load hardware", e);
            return ResponseEntity.internalServerError().body("전자부하 제어 실패");
        }
    }

    /**
     * Comprehensive BMS settings control (all settings at once)
     */
    @PostMapping("/hardware/settings/all")
    public ResponseEntity<String> setAllBmsSettingsHardware(@RequestBody BmsControlDto settingsDto) {
        try {
            // Input validation
            String validationError = validateThresholdSettings(settingsDto);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(validationError);
            }

            // Validate delay settings if provided
            if (settingsDto.getVoltageDelay() != null) {
                int delay = settingsDto.getVoltageDelay();
                if (delay < 2 || delay > 7) {
                    return ResponseEntity.badRequest().body("전압 딜레이는 2초 ~ 7초 범위여야 합니다.");
                }
            }

            if (settingsDto.getChargeCurrentDelay() != null || settingsDto.getChargeCurrentRelease() != null) {
                Integer delay = settingsDto.getChargeCurrentDelay();
                Integer release = settingsDto.getChargeCurrentRelease();
                if (delay == null || release == null) {
                    return ResponseEntity.badRequest().body("충전 전류 딜레이와 해제 시간을 모두 제공해야 합니다.");
                }
                if (delay < 5 || delay > 15 || release < 10 || release > 32) {
                    return ResponseEntity.badRequest().body("충전 전류 딜레이 범위 오류: 딜레이(5-15초), 해제(10-32초)");
                }
            }

            if (settingsDto.getDischargeCurrentDelay() != null || settingsDto.getDischargeCurrentRelease() != null) {
                Integer delay = settingsDto.getDischargeCurrentDelay();
                Integer release = settingsDto.getDischargeCurrentRelease();
                if (delay == null || release == null) {
                    return ResponseEntity.badRequest().body("방전 전류 딜레이와 해제 시간을 모두 제공해야 합니다.");
                }
                if (delay < 5 || delay > 15 || release < 10 || release > 32) {
                    return ResponseEntity.badRequest().body("방전 전류 딜레이 범위 오류: 딜레이(5-15초), 해제(10-32초)");
                }
            }

            // Apply all settings using hardware API
            boolean allSuccess = true;
            StringBuilder resultMessage = new StringBuilder("BMS 설정 결과:\n");
            
            // Voltage thresholds
            if (settingsDto.getOverchargeVoltage() != null) {
                boolean success = bmsService.setOverchargeVoltageHardware(settingsDto.getOverchargeVoltage());
                resultMessage.append("- 과충전 임계값: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }

            if (settingsDto.getUnderchargeVoltage() != null) {
                boolean success = bmsService.setUnderchargeVoltageHardware(settingsDto.getUnderchargeVoltage());
                resultMessage.append("- 과방전 임계값: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }

            // Current thresholds
            if (settingsDto.getOverchargeCurrent() != null) {
                boolean success = bmsService.setOverchargeCurrentHardware(settingsDto.getOverchargeCurrent());
                resultMessage.append("- 과충전 전류값: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }

            if (settingsDto.getDischargeCurrent() != null) {
                boolean success = bmsService.setDischargeCurrentHardware(settingsDto.getDischargeCurrent());
                resultMessage.append("- 과방전 전류값: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }

            // Delay settings
            if (settingsDto.getVoltageDelay() != null) {
                boolean success = bmsService.setVoltageDelayHardware(settingsDto.getVoltageDelay());
                resultMessage.append("- 전압 딜레이: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }

            if (settingsDto.getChargeCurrentDelay() != null && settingsDto.getChargeCurrentRelease() != null) {
                boolean success = bmsService.setChargeCurrentDelayHardware(
                    settingsDto.getChargeCurrentDelay(), settingsDto.getChargeCurrentRelease());
                resultMessage.append("- 충전 전류 딜레이: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }

            if (settingsDto.getDischargeCurrentDelay() != null && settingsDto.getDischargeCurrentRelease() != null) {
                boolean success = bmsService.setDischargeCurrentDelayHardware(
                    settingsDto.getDischargeCurrentDelay(), settingsDto.getDischargeCurrentRelease());
                resultMessage.append("- 방전 전류 딜레이: ").append(success ? "성공" : "실패").append("\n");
                if (!success) allSuccess = false;
            }
            
            if (allSuccess) {
                resultMessage.append("\n모든 설정이 성공적으로 적용되었습니다.");
                return ResponseEntity.ok(resultMessage.toString());
            } else {
                resultMessage.append("\n일부 설정이 실패했습니다.");
                return ResponseEntity.status(207).body(resultMessage.toString()); // 207 Multi-Status
            }

        } catch (Exception e) {
            log.error("Error setting all BMS hardware settings", e);
            return ResponseEntity.internalServerError().body("BMS 설정 실패: " + e.getMessage());
        }
    }

    /**
     * Get available hardware control functions and their status
     */
    @GetMapping("/hardware/capabilities")
    public ResponseEntity<java.util.Map<String, Object>> getHardwareCapabilities() {
        java.util.Map<String, Object> capabilities = new java.util.HashMap<>();
        
        // System capabilities
        capabilities.put("springBootBackend", true);
        capabilities.put("mqttCommunication", true);
        capabilities.put("webSocketRealTime", true);
        capabilities.put("databaseStorage", true);
        
        // Python hardware controller capabilities
        boolean pythonAvailable = bmsService.isHardwareControllerAvailable();
        capabilities.put("pythonHardwareController", pythonAvailable);
        
        if (pythonAvailable) {
            java.util.Map<String, Object> hardwareFunctions = new java.util.HashMap<>();
            hardwareFunctions.put("set_OV", "Overcharge voltage threshold control");
            hardwareFunctions.put("set_UV", "Undercharge voltage threshold control");
            hardwareFunctions.put("set_ChgOC", "Overcharge current threshold control");
            hardwareFunctions.put("set_DsgOC", "Discharge current threshold control");
            hardwareFunctions.put("set_delayVoltage", "Voltage delay timing control");
            hardwareFunctions.put("set_delayChgOC", "Charge current delay/release control");
            hardwareFunctions.put("set_delayDsgOC", "Discharge current delay/release control");
            hardwareFunctions.put("Reset_settings", "Reset all settings to defaults");
            hardwareFunctions.put("FET_control", "Direct charge/discharge FET control");
            hardwareFunctions.put("electronic_load_control", "Electronic load CC/CP mode control");
            
            capabilities.put("availableHardwareFunctions", hardwareFunctions);
            
            java.util.Map<String, String> endpoints = new java.util.HashMap<>();
            endpoints.put("voltage_thresholds", "POST /api/bms/settings/{overcharge-voltage,undercharge-voltage}");
            endpoints.put("current_thresholds", "POST /api/bms/settings/{overcharge-current,discharge-current}");
            endpoints.put("delay_settings", "POST /api/bms/settings/{voltage-delay,charge-current-delay,discharge-current-delay}");
            endpoints.put("comprehensive_settings", "POST /api/bms/settings/{thresholds,all}");
            endpoints.put("fet_control", "POST /api/bms/hardware/fet/control");
            endpoints.put("electronic_load", "POST /api/bms/hardware/electronic-load/control");
            endpoints.put("settings_reset", "POST /api/bms/settings/reset");
            endpoints.put("status_monitoring", "GET /api/bms/{status,hardware/status}");
            
            capabilities.put("availableEndpoints", endpoints);
        } else {
            capabilities.put("availableHardwareFunctions", "Python hardware controller not available - MQTT fallback only");
            capabilities.put("availableEndpoints", "Limited to MQTT-based control");
        }
        
        return ResponseEntity.ok(capabilities);
    }
}
