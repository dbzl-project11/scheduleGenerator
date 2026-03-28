package org.dbzl.schedule;

import org.dbzl.domain.Division;
import org.dbzl.domain.Match;
import org.dbzl.domain.Team;
import org.dbzl.writer.MarkdownWriter;
import org.dbzl.writer.TextWriter;

import java.util.*;


public class ScheduleGenerator {

    //to adjust for new seasons, the only thing that *should* need to be done is rearrange the teams into the right Kais
    private static final List<String> northKaiTeams = List.of("Primal Instinct", "Z-fighters", "Master & Student");
    private static final List<String> eastKaiTeams = List.of("Sentai Squad",  "Tiny Terrors", "Malevolent Souls");
    private static final List<String> westKaiTeams = List.of("Time Patrol", "Budokai", "Cinema");
    private static final List<String> southKaiTeams = List.of("Creations", "Cold Kingdom", "Demons");

    public static void main(String [] args){

       ScheduleGenerator self = new ScheduleGenerator();
       self.generateSchedule(northKaiTeams.stream().map(team -> new Team(team, Division.NORTH_KAI)).toList(),
               eastKaiTeams.stream().map(team -> new Team(team, Division.EAST_KAI)).toList(),
               westKaiTeams.stream().map(team -> new Team(team, Division.WEST_KAI)).toList(),
               southKaiTeams.stream().map(team -> new Team(team, Division.SOUTH_KAI)).toList());

    }

    private void generateSchedule(List<Team> northKai, List<Team> eastKai,List<Team> westKai,List<Team> southKai){
         List<Team> allTeams = new ArrayList<>();
         allTeams.addAll(northKai);
         allTeams.addAll(eastKai);
         allTeams.addAll(westKai);
         allTeams.addAll(southKai);
        System.out.println("Generating matches");
        Map<Integer, List<Match>> seasonSchedule = buildSchedule(allTeams);

        TextWriter textWriter = new TextWriter();
        textWriter.writeToFile(seasonSchedule, allTeams, "./schedule.txt");

        MarkdownWriter markdownWriter = new MarkdownWriter();
        markdownWriter.printScheduleByTeam(allTeams, "./scheduleMarkdown.md");
        markdownWriter.printScheduleByWeek(seasonSchedule, "./scheduleByWeek.md");
    }

    public Map<Integer, List<Match>> buildSchedule(List<Team> allTeams) {
        List<Match> mainSeasonMatches;
        mainSeasonMatches = MatchBuilder.buildAllPairings(allTeams);
        System.out.println("Pairings built, moving to weekly schedule");
        Map<Integer, List<Match>> seasonSchedule;
        WeekScheduler weekScheduler = new WeekScheduler();
        //basically - keep trying till it works. locally, it took < ~10 seconds to generate a full schedule.
        //the retry is mostly caused by the rng trying to work with the scheduling constraints.
        do{
            seasonSchedule = weekScheduler.buildShortSchedule(mainSeasonMatches, allTeams);
        } while(seasonSchedule.entrySet().stream().anyMatch(week -> week.getValue().size() < 6));

        allTeams.sort(Comparator.comparing(team -> team.getDivision().ordinal()));
        System.out.println("Schedule by team");
        allTeams.forEach(team -> System.out.println(team.printSchedule()));
        System.out.println("North Kai teams with 6 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.NORTH_KAI && team.getHomeGamesCount()==6).count());
        System.out.println("North Kai teams with 5 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.NORTH_KAI && team.getHomeGamesCount()==5).count());
        System.out.println();
        System.out.println("East Kai teams with 6 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.EAST_KAI && team.getHomeGamesCount()==6).count());
        System.out.println("East Kai teams with 5 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.EAST_KAI && team.getHomeGamesCount()==5).count());
        System.out.println();
        System.out.println("West Kai teams with 6 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.WEST_KAI && team.getHomeGamesCount()==6).count());
        System.out.println("West Kai teams with 5 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.WEST_KAI && team.getHomeGamesCount()==5).count());
        System.out.println();
        System.out.println("South Kai teams with 6 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.SOUTH_KAI && team.getHomeGamesCount()==6).count());
        System.out.println("South Kai teams with 5 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.SOUTH_KAI && team.getHomeGamesCount()==5).count());

        seasonSchedule.forEach((week, matches) -> Collections.shuffle(matches));
        return seasonSchedule;
    }

}
