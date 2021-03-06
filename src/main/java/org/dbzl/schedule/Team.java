package org.dbzl.schedule;

import java.util.*;

public class Team {

    private String name;
    private Division division;
    private List<Match> schedule;
    private int homeGames =0;

    public Team(String name, Division division) {
        this.name = name;
        this.division = division;
        this.schedule = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Division getDivision() {
        return division;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public List<Match> getSchedule() {
        schedule.sort(Comparator.comparingInt(Match::getWeek));
        return schedule;
    }

    public void setSchedule(List<Match> schedule) {
        this.schedule = schedule;
    }

    public boolean hasFoughtTeam(String opposingTeamName){
        return getSchedule().stream().anyMatch(match ->
                match.getAwayTeam().getName().equals(opposingTeamName) || match.getHomeTeam().getName().equals(opposingTeamName));
    }

    public boolean hasBeenPairedForWeek(int week){
        return getSchedule().stream().anyMatch(match ->  match.getWeek() == week);
    }

    public void addHomeGame(){
        homeGames++;
    }

    public int getHomeGames() {
        return homeGames;
    }

    public void addMatchToSchedule(Match match){
        schedule.add(match);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return Objects.equals(name, team.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, division, schedule, homeGames);
    }
}
