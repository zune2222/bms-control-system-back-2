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
            BmsControlDto controlDto = objectMapper.readValue(payload, BmsControlDto.class);
            
            // WebSocket을 통해 프론트엔드로 제어 상태 전송
            messagingTemplate.convertAndSend("/topic/bms-control", controlDto);
            
            log.info("BMS control message received and broadcasted: {}", controlDto);
        } catch (Exception e) {
            log.error("Error handling BMS control message", e);
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

    public void sendControlCommand(BmsControlDto controlDto) {
        try {
            // MQTT를 통해 라즈베리파이로 제어 명령 전송
            String mqttPayload = objectMapper.writeValueAsString(controlDto);
            
            // MQTT 아웃바운드 채널을 통해 전송
            Message<String> message = MessageBuilder.withPayload(mqttPayload).build();
            mqttOutboundChannel.send(message);
            
            // 로그 추가
            log.info("Control command sent via MQTT: {}", controlDto);
            log.info("MQTT payload: {}", mqttPayload);
            
        } catch (Exception e) {
            log.error("Error sending control command", e);
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
}
