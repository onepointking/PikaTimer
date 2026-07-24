package com.pikatimer.participant;

import com.pikatimer.race.Wave;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.HashMap;
import java.util.Map;

public class Participant {
    private String bib;
    private String sex = "";
    private Integer age;
    private final Map<Integer, StringProperty> customAttributes = new HashMap<>();
    private final ObservableList<Wave> waves = FXCollections.observableArrayList();

    public String getBib() {
        return bib;
    }

    public void setBib(String bib) {
        this.bib = bib;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public StringProperty getCustomAttribute(Integer id) {
        customAttributes.putIfAbsent(id, new SimpleStringProperty(""));
        return customAttributes.get(id);
    }

    public String getNamedAttribute(String attribute) {
        if ("sex".equals(attribute) || "sex-gender".equals(attribute)) {
            return sex;
        }
        if ("age".equals(attribute)) {
            return age == null ? "" : age.toString();
        }
        return "";
    }

    public ObservableList<Wave> wavesObservableList() {
        return waves;
    }
}
