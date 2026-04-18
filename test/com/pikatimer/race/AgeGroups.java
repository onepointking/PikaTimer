package com.pikatimer.race;

public class AgeGroups {
    public String ageToAGString(Integer age) {
        if (age == null || age <= 0) {
            return "0";
        }
        int start = (age / 5) * 5;
        return start + "-" + (start + 4);
    }
}
