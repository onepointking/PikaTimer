package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import com.pikatimer.results.ProcessedResult;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javafx.util.Pair;
import org.junit.Assert;
import org.junit.Test;

public class AwardCategoryAgeGroupWinnersTest {

    @Test
    public void ageGroupCategorySelectsFastestRunnerPerSexAndAgeGroup() {
        AwardCategory category = new AwardCategory();
        category.setType(AwardCategoryType.AGEGROUP);
        category.setDepth(1);

        Race race = createRace();
        RaceAwards raceAwards = new RaceAwards();
        raceAwards.setRace(race);
        category.setRaceAward(raceAwards);

        ProcessedResult maleWinner = result("m1", "M", "30-34", 30, Duration.ofMinutes(20));
        ProcessedResult maleSecond = result("m2", "M", "30-34", 31, Duration.ofMinutes(21));
        ProcessedResult femaleWinner = result("f1", "F", "30-34", 32, Duration.ofMinutes(19));
        ProcessedResult femaleMastersWinner = result("f2", "F", "40-44", 41, Duration.ofMinutes(22));
        ProcessedResult femaleMastersSecond = result("f3", "F", "40-44", 42, Duration.ofMinutes(23));

        Pair<Map<String, List<AwardWinner>>, List<ProcessedResult>> result = category.process(
                Arrays.asList(maleSecond, femaleMastersSecond, maleWinner, femaleWinner, femaleMastersWinner)
        );

        Assert.assertEquals("m1", result.getKey().get("Male 30-34").get(0).participant.getBib());
        Assert.assertEquals("f1", result.getKey().get("Female 30-34").get(0).participant.getBib());
        Assert.assertEquals("f2", result.getKey().get("Female 40-44").get(0).participant.getBib());
        Assert.assertEquals(2, result.getValue().size());
    }

    @Test
    public void ageGroupCategoryRespectsDepthWithinEachSubcategory() {
        AwardCategory category = new AwardCategory();
        category.setType(AwardCategoryType.AGEGROUP);
        category.setDepth(2);

        Race race = createRace();
        RaceAwards raceAwards = new RaceAwards();
        raceAwards.setRace(race);
        category.setRaceAward(raceAwards);

        ProcessedResult first = result("m1", "M", "30-34", 30, Duration.ofMinutes(20));
        ProcessedResult second = result("m2", "M", "30-34", 31, Duration.ofMinutes(21));
        ProcessedResult third = result("m3", "M", "30-34", 32, Duration.ofMinutes(22));

        Pair<Map<String, List<AwardWinner>>, List<ProcessedResult>> result = category.process(Arrays.asList(third, second, first));

        List<AwardWinner> maleWinners = result.getKey().get("Male 30-34");
        Assert.assertEquals(2, maleWinners.size());
        Assert.assertEquals("m1", maleWinners.get(0).participant.getBib());
        Assert.assertEquals(Integer.valueOf(1), maleWinners.get(0).awardPlace);
        Assert.assertEquals("m2", maleWinners.get(1).participant.getBib());
        Assert.assertEquals(Integer.valueOf(2), maleWinners.get(1).awardPlace);
        Assert.assertEquals(1, result.getValue().size());
        Assert.assertEquals("m3", result.getValue().get(0).getParticipant().getBib());
    }

    private Race createRace() {
        Race race = new Race();
        race.setStringAttribute("TimeDisplayFormat", "HH:MM:SS");
        race.setStringAttribute("TimeRoundingMode", "Down");
        race.setBooleanAttribute("permitTies", false);
        return race;
    }

    private ProcessedResult result(String bib, String sex, String agCode, int age, Duration chipFinish) {
        Participant participant = new Participant();
        participant.setBib(bib);
        participant.setSex(sex);
        participant.setAge(age);

        ProcessedResult processedResult = new ProcessedResult();
        processedResult.setParticipant(participant);
        processedResult.setAGCode(agCode);
        processedResult.setChipFinish(chipFinish);
        processedResult.setGunFinish(chipFinish.plusSeconds(5));
        return processedResult;
    }
}
