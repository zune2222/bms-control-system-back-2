package com.bms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PythonHardwareClient {

    private final RestTemplate restTemplate;

    @Value("${bms.python.hardware.url:http://localhost:8001}")
    private String pythonHardwareUrl;

    @Value("${bms.python.hardware.timeout:5000}")
    private int timeoutMs;

    @Value("${bms.python.hardware.enabled:true}")
    private boolean hardwareClientEnabled;

    /**
     * Check Python hardware controller health
     */
    public boolean isHardwareControllerAvailable() {
        if (!hardwareClientEnabled) {
            log.debug("Python hardware client is disabled");
            return false;
        }

        try {
            String url = pythonHardwareUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> health = response.getBody();
                log.debug("Hardware controller health: {}", health);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.warn("Python hardware controller not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set overcharge voltage threshold (set_OV)
     */
    public boolean setOverchargeVoltage(double voltage) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/overcharge-voltage")
                .queryParam("voltage", voltage)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set overcharge voltage to {}V via Python API", voltage);
                return true;
            } else {
                log.error("Failed to set overcharge voltage: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting overcharge voltage via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set undercharge voltage threshold (set_UV)
     */
    public boolean setUnderchargeVoltage(double voltage) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/undercharge-voltage")
                .queryParam("voltage", voltage)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set undercharge voltage to {}V via Python API", voltage);
                return true;
            } else {
                log.error("Failed to set undercharge voltage: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting undercharge voltage via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set overcharge current threshold (set_ChgOC)
     */
    public boolean setOverchargeCurrent(double current) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/overcharge-current")
                .queryParam("current", current)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set overcharge current to {}A via Python API", current);
                return true;
            } else {
                log.error("Failed to set overcharge current: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting overcharge current via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set discharge current threshold (set_DsgOC)
     */
    public boolean setDischargeCurrent(double current) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/discharge-current")
                .queryParam("current", current)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set discharge current to {}A via Python API", current);
                return true;
            } else {
                log.error("Failed to set discharge current: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting discharge current via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set voltage delay time (set_delayVoltage)
     */
    public boolean setVoltageDelay(int delay) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/voltage-delay")
                .queryParam("delay", delay)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set voltage delay to {}s via Python API", delay);
                return true;
            } else {
                log.error("Failed to set voltage delay: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting voltage delay via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set charge current delay/release times (set_delayChgOC)
     */
    public boolean setChargeCurrentDelay(int delay, int release) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/charge-current-delay")
                .queryParam("delay", delay)
                .queryParam("release", release)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set charge current delay to {}s/{}s via Python API", delay, release);
                return true;
            } else {
                log.error("Failed to set charge current delay: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting charge current delay via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Set discharge current delay/release times (set_delayDsgOC)
     */
    public boolean setDischargeCurrentDelay(int delay, int release) {
        try {
            String url = UriComponentsBuilder
                .fromHttpUrl(pythonHardwareUrl + "/api/bms/settings/discharge-current-delay")
                .queryParam("delay", delay)
                .queryParam("release", release)
                .toUriString();
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully set discharge current delay to {}s/{}s via Python API", delay, release);
                return true;
            } else {
                log.error("Failed to set discharge current delay: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error setting discharge current delay via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Reset all BMS settings to defaults (Reset_settings)
     */
    public boolean resetSettings() {
        try {
            String url = pythonHardwareUrl + "/api/bms/settings/reset";
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully reset BMS settings via Python API");
                return true;
            } else {
                log.error("Failed to reset BMS settings: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error resetting BMS settings via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Control BMS FETs directly
     */
    public boolean controlFET(Boolean chargeFetStatus, Boolean dischargeFetStatus) {
        try {
            String url = pythonHardwareUrl + "/api/bms/fet/control";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "charge_fet_status", chargeFetStatus != null ? chargeFetStatus : false,
                "discharge_fet_status", dischargeFetStatus != null ? dischargeFetStatus : false
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully controlled FET via Python API: charge={}, discharge={}", 
                    chargeFetStatus, dischargeFetStatus);
                return true;
            } else {
                log.error("Failed to control FET: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error controlling FET via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Control electronic load
     */
    public boolean controlElectronicLoad(Boolean enabled, String loadMode, Integer cpModeLevel) {
        try {
            String url = pythonHardwareUrl + "/api/electronic-load/control";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of(
                "electronicLoadEnabled", enabled != null ? enabled : false,
                "loadMode", loadMode != null ? loadMode : "CC",
                "cpModeLevel", cpModeLevel != null ? cpModeLevel : 1
            );
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Successfully controlled electronic load via Python API: enabled={}, mode={}, level={}", 
                    enabled, loadMode, cpModeLevel);
                return true;
            } else {
                log.error("Failed to control electronic load: HTTP {}", response.getStatusCode());
                return false;
            }
        } catch (RestClientException e) {
            log.error("Error controlling electronic load via Python API: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get BMS status from Python hardware controller
     */
    public Map<String, Object> getBmsStatus() {
        try {
            String url = pythonHardwareUrl + "/api/bms/status";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Successfully retrieved BMS status from Python API");
                return response.getBody();
            } else {
                log.error("Failed to get BMS status: HTTP {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error getting BMS status from Python API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get BMS settings from Python hardware controller
     */
    public Map<String, Object> getBmsSettings() {
        try {
            String url = pythonHardwareUrl + "/api/bms/settings";
            
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Successfully retrieved BMS settings from Python API");
                return response.getBody();
            } else {
                log.error("Failed to get BMS settings: HTTP {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error getting BMS settings from Python API: {}", e.getMessage());
            return null;
        }
    }
}