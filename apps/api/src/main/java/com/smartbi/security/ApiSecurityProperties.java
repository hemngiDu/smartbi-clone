package com.smartbi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties("smartbi.security")
public class ApiSecurityProperties {
    private Map<String, String> apiKeys = new LinkedHashMap<>();
    private Map<String, String> roles = new LinkedHashMap<>();
    public Map<String, String> getApiKeys() { return apiKeys; }
    public void setApiKeys(Map<String, String> apiKeys) { this.apiKeys = apiKeys; }
    public Map<String, String> getRoles() { return roles; }
    public void setRoles(Map<String, String> roles) { this.roles = roles; }
}
