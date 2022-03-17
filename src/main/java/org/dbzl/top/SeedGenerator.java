package org.dbzl.top;

import org.dbzl.domain.Division;
import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SeedGenerator {

    public static void main(String [] args) throws IOException {

        List<String> lines = Files.readAllLines(Paths.get("teams.txt"));
        List<String> matchLines = Files.readAllLines(Paths.get("matches.txt"));
        List<Team> teams = parseTeams(lines);
        parseMatches(teams, matchLines);
        //set the initial team weights before calculating the Strength of schedule tiebreakers.
        calculateTeamStrenth(teams);
        teams.sort(Comparator.comparingInt(Team::getWins).thenComparing(Team::getTeamWeight).thenComparing(Team::getScheduleStrength).thenComparing(Team::getWinMarginTieBreaker).reversed());
        teams.forEach(team -> System.out.println(team.getName() + ", " + team.getWins() + ", " + (team.getScheduleStrength()) + ", " + team.getWinMarginTieBreaker()));
    }

    public static void calculateTeamStrenth(List<Team> teams){
        for(Team team : teams) {
            double initialTeamWeight = 10 * team.getWins();
            team.setTeamWeight(initialTeamWeight);
        }
        for(Team team : teams){
            double scheduleStrength = 0;
            for(Match match : team.getSchedule()){
                if(match.getHomeTeam().equals(team)){
                    scheduleStrength += match.getAwayTeam().getTeamWeight();
                } else{
                    scheduleStrength += match.getHomeTeam().getTeamWeight();
                }
            }
           team.setScheduleStrength(scheduleStrength);
        }

    }

    public static List<Team> parseTeams(List<String> fileLines){
        List<Team> teams = new ArrayList<>();
        for(String line : fileLines){
            teams.add(new Team(line.trim().toUpperCase(), Division.NORTH_KAI));
        }

        return teams;
    }


    // file format: week,team 1,team 2, team that won (1 or 2), win margin (health bars)\
    public static void parseMatches(List<Team> teams, List<String> lines){
        Map<String, Team> teamsByName = teams.stream().collect(Collectors.toMap(Team::getName, Function.identity()));
        for(String line : lines){
            if(line.isEmpty() || line.isBlank()){
                continue;
            }
            System.out.println("Processing "+ line);
            String [] parts = line.split(",");
            int week = Integer.parseInt(parts[0]);
            Team homeTeam = teamsByName.get(parts[1].trim().toUpperCase());
            Team awayTeam = teamsByName.get(parts[2].trim().toUpperCase());
            Match match = new Match(homeTeam, week, awayTeam);
            homeTeam.addMatchToSchedule(match);
            awayTeam.addMatchToSchedule(match);
            match.setWinningTeam(Integer.parseInt(parts[3]));
            match.setWinMargin(Double.parseDouble(parts[4]));
        }
    }

}
