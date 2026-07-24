package com.pikatimer.results;

import java.util.ArrayList;
import java.util.List;

public class ResultsDAO {
    private static final ResultsDAO INSTANCE = new ResultsDAO();

    public static ResultsDAO getInstance() {
        return INSTANCE;
    }

    public List<RawResult> getResults(Integer raceId) {
        return new ArrayList<>();
    }

    public static class RawResult {
        private final String bib;

        public RawResult(String bib) {
            this.bib = bib;
        }

        public String getBib() {
            return bib;
        }
    }
}
