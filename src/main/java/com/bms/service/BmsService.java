package com.bms.service;

import com.bms.dto.BmsControlDto;
import com.bms.dto.BmsStatusDto;
import com.bms.model.BmsData;
import com.bms.repository.BmsDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BmsService {

    private final BmsDataRepository bmsDataRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final org.springframework.messaging.MessageChannel mqttOutboundChannel;
    private final PythonHardwareClient pythonHardwareClient;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<?> message) {
        try {
            String payload;
            Object messagePayload = message.getPayload();
            
            // payload가 byte 배열인지 String인지 확인
            if (messagePayload instanceof byte[]) {
                payload = new String((byte[]) messagePayload);
            } else {
                payload = messagePayload.toString();
            }
            
            String topic = message.getHeaders().get("mqtt_receivedTopic").toString();
            
            log.info("Received MQTT message from topic: {}, payload: {}", topic, payload);
            
            if (topic.contains("bms/status")) {
                handleBmsStatusMessage(payload);
            } else if (topic.contains("bms/control")) {
                handleBmsControlMessage(payload);
            } else if (topic.contains("bms/fet/status")) {
                handleBmsFetStatusMessage(payload);
            } else if (topic.contains("electronic_load/control")) {
                handleElectronicLoadControlMessage(payload);
            }
        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }

    private void handleBmsStatusMessage(String payload) {
        try {
            BmsStatusDto statusDto = objectMapper.readValue(payload, BmsStatusDto.class);
            
            // BMS 데이터를 엔티티로 변환하여 저장
            BmsData bmsData = new BmsData();
            bmsData.setTotalVoltage(statusDto.getTotalVoltage());
            bmsData.setCurrent(statusDto.getCurrent());
            bmsData.setTemperature(statusDto.getTemperature());
            bmsData.setRemainingCapacity(statusDto.getRemainingCapacity());
            bmsData.setChargeFetStatus(statusDto.getChargeFetStatus());
            bmsData.setDischargeFetStatus(statusDto.getDischargeFetStatus());
            bmsData.setCellVoltages(statusDto.getCellVoltages());
            bmsData.setTimestamp(LocalDateTime.now());
            
            bmsDataRepository.save(bmsData);
            
            // WebSocket을 통해 프론트엔드로 실시간 데이터 전송
            messagingTemplate.convertAndSend("/topic/bms-status", statusDto);
            log.info("WebSocket message sent to /topic/bms-status");
            
            log.info("BMS status data saved and broadcasted: {}", statusDto);
        } catch (Exception e) {
            log.error("Error handling BMS status message", e);
        }
    }

    private void handleBmsControlMessage(String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            BmsControlDto controlDto = mapper.readValue(payload, BmsControlDto.class);
            
            log.info("Received BMS control command: {}", controlDto);
            
            // null 값 처리: 기존 상태 유지
            if (controlDto.getChargeFetStatus() != null) {
                log.info("Setting charge FET status to: {}", controlDto.getChargeFetStatus());
            }
            if (controlDto.getDischargeFetStatus() != null) {
                log.info("Setting discharge FET status to: {}", controlDto.getDischargeFetStatus());
            }
            
            // WebSocket으로 제어 명령 전송
            messagingTemplate.convertAndSend("/topic/bms-control", controlDto);
            log.info("Control command sent via WebSocket: {}", controlDto);
            
        } catch (Exception e) {
            log.error("Error processing BMS control message: {}", e.getMessage(), e);
        }
    }

    private void handleBmsFetStatusMessage(String payload) {
        try {
            BmsControlDto fetStatusDto = objectMapper.readValue(payload, BmsControlDto.class);
            
            // WebSocket을 통해 프론트엔드로 FET 상태 전송
            messagingTemplate.convertAndSend("/topic/bms-fet-status", fetStatusDto);
            
            log.info("BMS FET status message received and broadcasted: {}", fetStatusDto);
        } catch (Exception e) {
            log.error("Error handling BMS FET status message", e);
        }
    }

    private void handleElectronicLoadControlMessage(String payload) {
        try {
            BmsControlDto controlDto = objectMapper.readValue(payload, BmsControlDto.class);
            
            log.info("Received Electronic Load control command: {}", controlDto);
            
            // WebSocket으로 제어 명령 전송 (필요시)
            messagingTemplate.convertAndSend("/topic/electronic-load-control", controlDto);
            log.info("Electronic Load control command sent via WebSocket: {}", controlDto);
            
        } catch (Exception e) {
            log.error("Error processing Electronic Load control message: {}", e.getMessage(), e);
        }
    }

    public BmsStatusDto getLatestBmsStatus() {
        BmsData latestData = bmsDataRepository.findTopByOrderByTimestampDesc();
        if (latestData != null) {
            return convertToDto(latestData);
        }
        return null;
    }

    public List<BmsData> getBmsHistory(LocalDateTime start, LocalDateTime end) {
        return bmsDataRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    public List<BmsData> getTemperatureHistory(int limit) {
        return bmsDataRepository.findLatestTemperatureHistory(limit);
    }

    public void sendControlCommand(BmsControlDto controlDto) {
        sendMqttCommand(controlDto, "bms/control");
    }

    public void sendElectronicLoadCommand(BmsControlDto controlDto) {
        sendMqttCommand(controlDto, "electronic_load/control");
    }

    private void sendMqttCommand(BmsControlDto controlDto, String topic) {
        try {
            log.info("Starting to send command to topic {}: {}", topic, controlDto);
            
            // ObjectMapper null 체크
            if (objectMapper == null) {
                log.error("ObjectMapper is null");
                throw new RuntimeException("ObjectMapper is not initialized");
            }
            
            // MQTT를 통해 라즈베리파이로 제어 명령 전송
            log.info("Converting control command to JSON...");
            String mqttPayload = objectMapper.writeValueAsString(controlDto);
            log.info("JSON conversion successful. Payload: {}", mqttPayload);
            
            // MQTT 아웃바운드 채널 null 체크
            if (mqttOutboundChannel == null) {
                log.error("MQTT outbound channel is null");
                throw new RuntimeException("MQTT outbound channel is not initialized");
            }
            
            // MQTT 아웃바운드 채널을 통해 전송 (topic 헤더 추가)
            log.info("Creating MQTT message with topic {}...", topic);
            Message<String> message = MessageBuilder
                .withPayload(mqttPayload)
                .setHeader("mqtt_topic", topic)
                .build();
            log.info("Sending message via MQTT channel to topic {}...", topic);
            
            boolean sent = mqttOutboundChannel.send(message);
            if (!sent) {
                log.error("Failed to send message via MQTT channel");
                throw new RuntimeException("Failed to send message via MQTT channel");
            }
            
            log.info("Command sent successfully via MQTT to topic {}: {}", topic, controlDto);
            log.info("MQTT payload: {}", mqttPayload);
            
        } catch (Exception e) {
            log.error("Error sending command to topic {}: {}", topic, e.getMessage());
            log.error("Stack trace: ", e);
            throw new RuntimeException("Failed to send command to topic " + topic, e);
        }
    }

    private BmsStatusDto convertToDto(BmsData bmsData) {
        BmsStatusDto dto = new BmsStatusDto();
        dto.setTotalVoltage(bmsData.getTotalVoltage());
        dto.setCurrent(bmsData.getCurrent());
        dto.setTemperature(bmsData.getTemperature());
        dto.setRemainingCapacity(bmsData.getRemainingCapacity());
        dto.setChargeFetStatus(bmsData.getChargeFetStatus());
        dto.setDischargeFetStatus(bmsData.getDischargeFetStatus());
        dto.setCellVoltages(bmsData.getCellVoltages());
        dto.setTimestamp(bmsData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return dto;
    }

    // Hardware Control Methods using Python API
    
    /**
     * Set overcharge voltage using direct Python API call (set_OV)
     */
    public boolean setOverchargeVoltageHardware(double voltage) {
        log.info("Setting overcharge voltage to {}V using Python hardware API", voltage);
        
        // Try Python API first for direct hardware control
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setOverchargeVoltage(voltage);
            if (success) {
                log.info("✅ Overcharge voltage set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        // Fallback to MQTT
        return setOverchargeVoltageMqtt(voltage);
    }

    /**
     * Set undercharge voltage using direct Python API call (set_UV)
     */
    public boolean setUnderchargeVoltageHardware(double voltage) {
        log.info("Setting undercharge voltage to {}V using Python hardware API", voltage);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setUnderchargeVoltage(voltage);
            if (success) {
                log.info("✅ Undercharge voltage set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return setUnderchargeVoltageMqtt(voltage);
    }

    /**
     * Set overcharge current using direct Python API call (set_ChgOC)
     */
    public boolean setOverchargeCurrentHardware(double current) {
        log.info("Setting overcharge current to {}A using Python hardware API", current);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setOverchargeCurrent(current);
            if (success) {
                log.info("✅ Overcharge current set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return setOverchargeCurrentMqtt(current);
    }

    /**
     * Set discharge current using direct Python API call (set_DsgOC)
     */
    public boolean setDischargeCurrentHardware(double current) {
        log.info("Setting discharge current to {}A using Python hardware API", current);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setDischargeCurrent(current);
            if (success) {
                log.info("✅ Discharge current set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return setDischargeCurrentMqtt(current);
    }

    /**
     * Set voltage delay using direct Python API call (set_delayVoltage)
     */
    public boolean setVoltageDelayHardware(int delay) {
        log.info("Setting voltage delay to {}s using Python hardware API", delay);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setVoltageDelay(delay);
            if (success) {
                log.info("✅ Voltage delay set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return setVoltageDelayMqtt(delay);
    }

    /**
     * Set charge current delay using direct Python API call (set_delayChgOC)
     */
    public boolean setChargeCurrentDelayHardware(int delay, int release) {
        log.info("Setting charge current delay to {}s/{}s using Python hardware API", delay, release);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setChargeCurrentDelay(delay, release);
            if (success) {
                log.info("✅ Charge current delay set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return setChargeCurrentDelayMqtt(delay, release);
    }

    /**
     * Set discharge current delay using direct Python API call (set_delayDsgOC)
     */
    public boolean setDischargeCurrentDelayHardware(int delay, int release) {
        log.info("Setting discharge current delay to {}s/{}s using Python hardware API", delay, release);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.setDischargeCurrentDelay(delay, release);
            if (success) {
                log.info("✅ Discharge current delay set successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return setDischargeCurrentDelayMqtt(delay, release);
    }

    /**
     * Reset all BMS settings using direct Python API call (Reset_settings)
     */
    public boolean resetSettingsHardware() {
        log.info("Resetting BMS settings using Python hardware API");
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.resetSettings();
            if (success) {
                log.info("✅ BMS settings reset successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        return resetSettingsMqtt();
    }

    /**
     * Control FET using direct Python API call
     */
    public boolean controlFETHardware(Boolean chargeFetStatus, Boolean dischargeFetStatus) {
        log.info("Controlling FET using Python hardware API: charge={}, discharge={}", chargeFetStatus, dischargeFetStatus);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.controlFET(chargeFetStatus, dischargeFetStatus);
            if (success) {
                log.info("✅ FET controlled successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        // Fallback to MQTT
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setChargeFetStatus(chargeFetStatus);
        controlDto.setDischargeFetStatus(dischargeFetStatus);
        sendControlCommand(controlDto);
        return true; // Assume MQTT success for now
    }

    /**
     * Control electronic load using direct Python API call
     */
    public boolean controlElectronicLoadHardware(Boolean enabled, String loadMode, Integer cpModeLevel) {
        log.info("Controlling electronic load using Python hardware API: enabled={}, mode={}, level={}", enabled, loadMode, cpModeLevel);
        
        if (pythonHardwareClient.isHardwareControllerAvailable()) {
            boolean success = pythonHardwareClient.controlElectronicLoad(enabled, loadMode, cpModeLevel);
            if (success) {
                log.info("✅ Electronic load controlled successfully via Python API");
                return true;
            } else {
                log.warn("⚠️ Python API call failed, falling back to MQTT");
            }
        } else {
            log.warn("⚠️ Python hardware controller not available, using MQTT fallback");
        }
        
        // Fallback to MQTT
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setElectronicLoadEnabled(enabled);
        controlDto.setLoadMode(loadMode);
        controlDto.setCpModeLevel(cpModeLevel);
        sendElectronicLoadCommand(controlDto);
        return true; // Assume MQTT success for now
    }

    // MQTT Fallback Methods (existing functionality)
    
    private boolean setOverchargeVoltageMqtt(double voltage) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_OV");
        controlDto.setOverchargeVoltage(voltage);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean setUnderchargeVoltageMqtt(double voltage) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_UV");
        controlDto.setUnderchargeVoltage(voltage);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean setOverchargeCurrentMqtt(double current) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_ChgOC");
        controlDto.setOverchargeCurrent(current);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean setDischargeCurrentMqtt(double current) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_DsgOC");
        controlDto.setDischargeCurrent(current);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean setVoltageDelayMqtt(int delay) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_delayVoltage");
        controlDto.setVoltageDelay(delay);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean setChargeCurrentDelayMqtt(int delay, int release) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_delayChgOC");
        controlDto.setChargeCurrentDelay(delay);
        controlDto.setChargeCurrentRelease(release);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean setDischargeCurrentDelayMqtt(int delay, int release) {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("set_delayDsgOC");
        controlDto.setDischargeCurrentDelay(delay);
        controlDto.setDischargeCurrentRelease(release);
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    private boolean resetSettingsMqtt() {
        BmsControlDto controlDto = new BmsControlDto();
        controlDto.setCommandType("Reset_settings");
        sendMqttCommand(controlDto, "bms/control");
        return true;
    }

    /**
     * Check if Python hardware controller is available
     */
    public boolean isHardwareControllerAvailable() {
        return pythonHardwareClient.isHardwareControllerAvailable();
    }

    /**
     * Get BMS status from Python hardware controller
     */
    public java.util.Map<String, Object> getBmsStatusFromHardware() {
        return pythonHardwareClient.getBmsStatus();
    }

    /**
     * Get BMS settings from Python hardware controller
     */
    public java.util.Map<String, Object> getBmsSettingsFromHardware() {
        return pythonHardwareClient.getBmsSettings();
    }
}
