package com.pikatimer.race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Race {
    private final Map<String, String> stringAttributes = new HashMap<>();
    private final Map<String, Boolean> booleanAttributes = new HashMap<>();
    private final SexGroups sexGroups = new SexGroups();
    private final AgeGroups ageGroups = new AgeGroups();
    private final List<com.pikatimer.timing.Split> splits = new ArrayList<>();

    public Long getRaceCutoff() {
        return 0L;
    }

    public String getStringAttribute(String key) {
        return stringAttributes.get(key);
    }

    public void setStringAttribute(String key, String value) {
        stringAttributes.put(key, value);
    }

    public Boolean getBooleanAttribute(String key) {
        return booleanAttributes.get(key);
    }

    public void setBooleanAttribute(String key, Boolean value) {
        booleanAttributes.put(key, value);
    }

    public SexGroups getSexGroups() {
        return sexGroups;
    }

    public AgeGroups getAgeGroups() {
        return ageGroups;
    }

    public List<com.pikatimer.timing.Split> getSplits() {
        return splits;
    }

    public Integer getID() {
        return 1;
    }
}
