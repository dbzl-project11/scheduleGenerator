package org.dbzl.schedule;

import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.util.*;

public class WeekScheduler {
    private static final Random rng = new Random();


    public Map<Integer, List<Match>> buildShortSchedule(List<Match> mainSeasonMatches, List<Team> allTeams) {
        if (allTeams.size() % 2 != 0) {
            throw new IllegalStateException("Short schedule requires an even number of teams, found: " + allTeams.size());
        }

        Map<Integer, List<Match>> seasonSchedule = new HashMap<>();
        List<Team> rotatingTeams = new ArrayList<>(allTeams);
        // Shuffle to vary the schedule between runs while maintaining valid round-robin structure
        Collections.shuffle(rotatingTeams, rng);

        Team anchor = rotatingTeams.remove(0);
        int numWeeks = allTeams.size() - 1;
        int teamsInRotation = rotatingTeams.size();

        // Implement Circle Method for Round Robin Scheduling
        for (int week = 1; week <= numWeeks; week++) {
            System.out.println("Building week : " + week);
            List<Match> weeklyMatches = new ArrayList<>();
            seasonSchedule.put(week, weeklyMatches);

            // 1. Anchor plays the team at the end of the rotation
            Team lastTeam = rotatingTeams.get(teamsInRotation - 1);
            weeklyMatches.add(findMatch(anchor, lastTeam, mainSeasonMatches));

            // 2. Pair the remaining teams
            for (int i = 0; i < teamsInRotation / 2; i++) {
                Team t1 = rotatingTeams.get(i);
                Team t2 = rotatingTeams.get(teamsInRotation - 2 - i);
                weeklyMatches.add(findMatch(t1, t2, mainSeasonMatches));
            }

            // 3. Set the week for all matches
            for (Match m : weeklyMatches) {
                m.setWeek(week);
            }

            // 4. Rotate the list: Move last element to the front
            Team movingTeam = rotatingTeams.remove(teamsInRotation - 1);
            rotatingTeams.add(0, movingTeam);
        }

        return seasonSchedule;
    }

    private Match findMatch(Team t1, Team t2, List<Match> matches) {
        return matches.stream()
                .filter(m -> (m.getHomeTeam().equals(t1) && m.getAwayTeam().equals(t2)) ||
                        (m.getHomeTeam().equals(t2) && m.getAwayTeam().equals(t1)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Match not found between " + t1.getName() + " and " + t2.getName()));
    }


}