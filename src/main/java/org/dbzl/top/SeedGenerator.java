package org.dbzl.top;

import org.dbzl.domain.Division;
import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SeedGenerator {

    public static void main(String [] args) throws IOException {

        List<String> lines = Files.readAllLines(Paths.get("teams.txt"));
        List<String> matchLines = Files.readAllLines(Paths.get("matches.txt"));
        List<Team> teams = parseTeams(lines);
        parseMatches(teams, matchLines);
        //set the initial team weights before calculating the Strength of schedule tie breakers.
        for(Team team : teams){
            double initialTeamWeight = (10 * team.getWins()) + (4* team.getAverageWinMargin());
            team.setTeamWeight(initialTeamWeight);
        }

        //now iterate over the teams and get the schedule strength
        for(Team team: teams){
            double scheduleStrength = 0;
            for(Match match : team.getSchedule()){

                scheduleStrength += match.getMatchWinner().getTeamWeight();
            }
            team.setTeamWeight(team.getTeamWeight() + scheduleStrength);
        }

        teams.sort(Comparator.comparingInt(Team::getWins).thenComparing(Team::getTeamWeight).reversed());
        teams.forEach(team -> System.out.println(team.getName() + ", " + team.getWins() + ", " + team.getTeamWeight()));
    }

    public static List<Team> parseTeams(List<String> fileLines){
        List<Team> teams = new ArrayList<>();
        for(String line : fileLines){
            teams.add(new Team(line.trim(), Division.NORTH_KAI));
        }

        return teams;
    }


    // file format: week,team 1,team 2, team that won (1 or 2), win margin (health bars)\
    public static void parseMatches(List<Team> teams, List<String> lines){
        Map<String, Team> teamsByName = teams.stream().collect(Collectors.toMap(Team::getName, Function.identity()));
        for(String line : lines){
            String [] parts = line.split(",");
            int week = Integer.valueOf(parts[0]);
            Team homeTeam = teamsByName.get(parts[1].trim());
            Team awayTeam = teamsByName.get(parts[2].trim());
            Match match = new Match(homeTeam, week, awayTeam);
            homeTeam.addMatchToSchedule(match);
            awayTeam.addMatchToSchedule(match);
            match.setWinningTeam(Integer.valueOf(parts[3]));
            match.setWinMargin(Double.valueOf(parts[4]));
        }
    }

}
