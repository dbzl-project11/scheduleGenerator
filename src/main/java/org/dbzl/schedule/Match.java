package org.dbzl.schedule;

public class Match {

    private  Team homeTeam;
    private int week;
    private Team opposingTeam;


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

    @Override
    public String toString() {
        return "Match{" +
                "homeTeam=" + homeTeam.getName() +
                ", week=" + week +
                ", opposingTeam=" + opposingTeam.getName() +
                '}';
    }
}
