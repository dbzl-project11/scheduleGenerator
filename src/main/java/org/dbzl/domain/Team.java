package org.dbzl.domain;

import java.util.*;
import java.util.stream.Collectors;

public class Team {

    private String name;
    private Division division;
    private List<Match> schedule;
    private double teamWeight;
    private double scheduleStrength;

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


    public long getHomeGamesCount() {
        return getSchedule().stream().filter(match -> match.getHomeTeam().getName().equals(getName())).count();
    }

    public List<Match> getHomeGames(){
        return getSchedule().stream().filter(match -> match.getHomeTeam().getName().equals(getName())).collect(Collectors.toList());

    }

    public long getDivisionalHomeGamesCount(){
        return getSchedule().stream().filter(match -> match.getHomeTeam().getName().equals(getName()) && match.isDivisionalMatch()).count();

    }

    public void addMatchToSchedule(Match match){
        schedule.add(match);
    }

    public void clearSchedule(){
        schedule.clear();
    }

    public double getTeamWeight() {
        return teamWeight;
    }

    public double getScheduleStrength() {
        return scheduleStrength;
    }

    public void setScheduleStrength(double scheduleStrength) {
        this.scheduleStrength = scheduleStrength;
    }

    public void setTeamWeight(double teamWeight) {
        this.teamWeight = teamWeight;
    }

    public String printSchedule(){
        StringBuilder builder = new StringBuilder();
        builder.append("Schedule for: ").append(name).append('\n');
        //use the getter so it's sorted
        getSchedule().forEach(match -> builder.append(match.getFullDescription()).append('\n'));
        builder.append("Home matches: ").append(getHomeGamesCount()).append('\n');
        return builder.toString();
    }

    public int getWins(){
        return  (int) schedule.stream().filter(match -> match.getMatchWinner().equals(this)).count();
    }

    public double getAverageWinMargin(){
        List<Match> wins = schedule.stream().filter(match -> match.getMatchWinner().equals(this)).toList();
        if(wins.isEmpty()){
            return 0;
        }
        return wins.stream().mapToDouble(Match::getWinMargin).sum()/wins.size();
    }

    public double getWinMarginTieBreaker(){
        return 4 * getAverageWinMargin();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return teamWeight == team.teamWeight && Objects.equals(name, team.name) && division == team.division;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, division);
    }
}
