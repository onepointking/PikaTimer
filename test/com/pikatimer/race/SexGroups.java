package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import java.util.ArrayList;
import java.util.List;

public class SexGroups {
    public List<String> listSexGroups(Participant participant) {
        List<String> groups = new ArrayList<>();
        if (participant == null || participant.getSex() == null || participant.getSex().isEmpty()) {
            return groups;
        }
        String sex = participant.getSex().toUpperCase();
        if (sex.startsWith("F")) {
            groups.add("Female");
        } else if (sex.startsWith("M")) {
            groups.add("Male");
        } else {
            groups.add(sex);
        }
        return groups;
    }

    public Boolean eligibilityFilter(Participant participant) {
        if (participant == null || participant.getSex() == null || participant.getSex().isEmpty()) {
            return false;
        }
        String sex = participant.getSex().toUpperCase();
        return sex.startsWith("M") || sex.startsWith("F");
    }
}
