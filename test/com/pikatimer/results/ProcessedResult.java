package com.pikatimer.results;

import com.pikatimer.participant.Participant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class ProcessedResult {
    private Participant participant;
    private String agCode;
    private Duration chipFinish;
    private Duration gunFinish;
    private final Map<Integer, Duration> splits = new HashMap<>();
    private final Map<Integer, Duration> segments = new HashMap<>();

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public String getAGCode() {
        return agCode;
    }

    public void setAGCode(String agCode) {
        this.agCode = agCode;
    }

    public Duration getChipFinish() {
        return chipFinish;
    }

    public void setChipFinish(Duration chipFinish) {
        this.chipFinish = chipFinish;
    }

    public Duration getGunFinish() {
        return gunFinish;
    }

    public void setGunFinish(Duration gunFinish) {
        this.gunFinish = gunFinish;
    }

    public Duration getSplit(Integer id) {
        return splits.get(id);
    }

    public Duration getSegmentTime(Integer id) {
        return segments.get(id);
    }
}
