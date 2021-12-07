package org.dbzl.domain;

import java.util.Objects;

public class Match {

    private  Team homeTeam;
    private int week;
    private Team opposingTeam;
    private int winningTeam;
    private double winMargin;



    public Match(Team homeTeam, int week, Team opposingTeam) {
        this.homeTeam = homeTeam;
        this.week = week;
        this.opposingTeam = opposingTeam;
    }

    public Team getHomeTeam() {
        return homeTeam;
    }

    public void setHomeTeam(Team homeTeam) {
        this.homeTeam = homeTeam;
    }

    public int getWeek() {
        return week;
    }

    public void setWeek(int week) {
        this.week = week;
    }

    public Team getAwayTeam() {
        return opposingTeam;
    }

    public boolean isDivisionalMatch(){
        return homeTeam.getDivision() == getAwayTeam().getDivision();
    }

    public void setOpposingTeam(Team opposingTeam) {
        this.opposingTeam = opposingTeam;
    }

    public String getMatchDescription(){

            return getHomeTeam().getName() + " vs " + getAwayTeam().getName() + (isDivisionalMatch() ? " " + homeTeam.getDivision().getAbbreviatedName() : "");
    }

    public String getFullDescription(){

        return week + " " + getHomeTeam().getName() + " vs " + getAwayTeam().getName() + (isDivisionalMatch() ? " " + homeTeam.getDivision().getAbbreviatedName() : "");
    }

    public void setWinningTeam(int winningTeam){
        this.winningTeam = winningTeam;
    }

    public Team getMatchWinner(){
        if(winningTeam == 1){
            return  homeTeam;
        } else if(winningTeam ==2){
            return opposingTeam;
        }
        return null;
    }

    public Match flipHomeAndAway(){
        return new Match(opposingTeam, week, homeTeam);
    }

    public double getWinMargin() {
        return winMargin;
    }

    public void setWinMargin(double winMargin) {
        this.winMargin = winMargin;
    }

    @Override
    public String toString() {
        return "Match{" +
                "homeTeam=" + homeTeam.getName() +
                ", week=" + week +
                ", opposingTeam=" + opposingTeam.getName() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return week == match.week && Objects.equals(homeTeam, match.homeTeam) && Objects.equals(opposingTeam, match.opposingTeam);
    }

    @Override
    public int hashCode() {
        return Objects.hash(homeTeam, week, opposingTeam);
    }
}
