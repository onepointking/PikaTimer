package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import java.util.ArrayList;
import java.util.List;

public class SexGroups {
    public List<String> listSexGroups(Participant participant) {
        List<String> groups = new ArrayList<>();
        if (participant.getSex().startsWith("F")) {
            groups.add("Female");
        } else {
            groups.add("Male");
        }
        return groups;
    }

    public Boolean eligibilityFilter(Participant participant) {
        return participant.getSex() != null && !participant.getSex().isEmpty();
    }
}
