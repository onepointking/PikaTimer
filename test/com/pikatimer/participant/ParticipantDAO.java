package com.pikatimer.participant;

import java.util.ArrayList;
import java.util.List;

public class ParticipantDAO {
    private static final ParticipantDAO INSTANCE = new ParticipantDAO();

    public static ParticipantDAO getInstance() {
        return INSTANCE;
    }

    public List<Participant> listParticipants() {
        return new ArrayList<>();
    }

    public Participant getParticipantByBib(String bib) {
        Participant participant = new Participant();
        participant.setBib(bib);
        return participant;
    }
}
