package com.pikatimer.race;

public class AgeGroups {
    public String ageToAGString(Integer age) {
        if (age == null || age <= 0) {
            return "0";
        }
        if (age <= 9) {
            return "1-9";
        }
        int start = (age / 5) * 5;
        return start + "-" + (start + 4);
    }
}
