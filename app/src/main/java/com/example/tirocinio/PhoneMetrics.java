package com.example.tirocinio;

import java.util.ArrayList;
import java.util.List;

public class PhoneMetrics {

    private String id;
    private String battery;
    private String availableMemory;
    private String totalMemory;
    private String downstreamBandwidth;
    private String upstreamBandwidth;
    private String signalStrength;
    private String isReachingInternet;
    private String isNotCongested;
    private String isNotSuspended;
    private String numOfCores;
    private String cpuFrequencies;
    private String cpuCacheSizes;
    private String cpuFlags;
    private String cpuUsage;
    private String delay;


    public PhoneMetrics(String id, String battery, String availableMemory, String totalMemory, String downstreamBandwidth, String upstreamBandwidth, String signalStrength, String isReachingInternet, String isNotCongested, String isNotSuspended, String numOfCores, String cpuFrequencies, String cpuCacheSizes, String cpuFlags, String cpuUsage, String delay){
        this.id=id;
        this.battery=battery;
        this.availableMemory=availableMemory;
        this.totalMemory=totalMemory;
        this.downstreamBandwidth=downstreamBandwidth;
        this.upstreamBandwidth=upstreamBandwidth;
        this.signalStrength=signalStrength;
        this.isReachingInternet=isReachingInternet;
        this.isNotCongested=isNotCongested;
        this.isNotSuspended=isNotSuspended;
        this.numOfCores=numOfCores;
        this.cpuFrequencies=cpuFrequencies;
        this.cpuCacheSizes=cpuCacheSizes;
        this.cpuFlags=cpuFlags;
        this.cpuUsage=cpuUsage;
        this.delay=delay;
    }

    public PhoneMetrics(){
        this.id="unknown";
        this.battery="unknown";
        this.availableMemory="unknown";
        this.totalMemory="unknown";
        this.downstreamBandwidth="unknown";
        this.upstreamBandwidth="unknown";
        this.signalStrength="unknown";
        this.isReachingInternet="unknown";
        this.isNotCongested="unknown";
        this.isNotSuspended="unknown";
        this.numOfCores="unknown";
        this.cpuFrequencies="unknown";
        this.cpuCacheSizes="unknown";
        this.cpuFlags="unknown";
        this.cpuUsage="unknown";
        this.delay="unknown";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() { return this.id; }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    public void setAvailableMemory(String availableMemory) {
        this.availableMemory = availableMemory;
    }

    public void setTotalMemory(String totalMemory) {
        this.totalMemory = totalMemory;
    }

    public void setDownstreamBandwidth(String downstreamBandwidth) {
        this.downstreamBandwidth = downstreamBandwidth;
    }

    public void setUpstreamBandwidth(String upstreamBandwidth) {
        this.upstreamBandwidth = upstreamBandwidth;
    }

    public void setSignalStrength(String signalStrength) {
        this.signalStrength = signalStrength;
    }

    public void setIsReachingInternet(String isReachingInternet) {
        this.isReachingInternet = isReachingInternet;
    }

    public void setIsNotCongested(String isNotCongested) {
        this.isNotCongested = isNotCongested;
    }

    public void setIsNotSuspended(String isNotSuspended) {
        this.isNotSuspended = isNotSuspended;
    }

    public void setNumOfCores(String numOfCores){ this.numOfCores = numOfCores; }

    public void setCpuFrequencies(String cpuFrequencies) {
        this.cpuFrequencies = cpuFrequencies;
    }

    public void setCpuCacheSizes(String cpuCacheSizes) { this.cpuCacheSizes = cpuCacheSizes; }

    public void setCpuFlags(String cpuFlags) { this.cpuFlags = cpuFlags; }

    public void setCpuUsage(String cpuUsage) { this.cpuUsage = cpuUsage; }

    public void setDelay(String delay) { this.delay = delay; }
}
