package com.step.tcd_rpkb.domain.model;

public class Credentials {
    private final String username;
    private final String password;
    private String deviceNum;

    public Credentials(String username, String password, String deviceNum) {
        this.username = username;
        this.password = password;
        this.deviceNum = deviceNum;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceNum() {
        return deviceNum;
    }

    public void setDeviceNum(String deviceNum) {
        this.deviceNum = deviceNum;
    }
}