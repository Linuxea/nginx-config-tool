package com.linuxea.nginx.script;

import java.util.Map;

public class ConfigDomain {

  private String config;

  private Map<String, String> methodMappingMap;

  public Map<String, String> getMethodMappingMap() {
    return methodMappingMap;
  }

  public void setMethodMappingMap(Map<String, String> methodMappingMap) {
    this.methodMappingMap = methodMappingMap;
  }

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }
}
