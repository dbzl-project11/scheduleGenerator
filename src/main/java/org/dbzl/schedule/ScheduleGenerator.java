package org.dbzl.schedule;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;


public class ScheduleGenerator {

    //to adjust for new seasons, the only thing that *should* need to be done is rearrange the teams into the right Kais
    private static final List<String> northKaiTeams = List.of("Buujins", "Budokai", "Androids", "Hybrids");
    private static final List<String> eastKaiTeams = List.of("Namek", "GT", "Kaiju", "Earth Defenders");
    private static final List<String> westKaiTeams = List.of("Royals", "Rugrats", "Cinema", "Resurrected Warriors");
    private static final List<String> southKaiTeams = List.of("Cold", "Muscle", "Sentai", "Derp");

    private static final Random rng = new Random();

    public static void main(String [] args){

        List<Team> allTeams = northKaiTeams.stream().map(team -> new Team(team, Division.NORTH_KAI)).collect(Collectors.toList());
        allTeams.addAll(eastKaiTeams.stream().map(team -> new Team(team, Division.EAST_KAI)).collect(Collectors.toList()));
        allTeams.addAll( westKaiTeams.stream().map(team -> new Team(team, Division.WEST_KAI)).collect(Collectors.toList()));
        allTeams.addAll(southKaiTeams.stream().map(team -> new Team(team, Division.SOUTH_KAI)).collect(Collectors.toList()));

        List<Match> mainSeasonMatches;
//        do {
            System.out.println("Generating matches");
            mainSeasonMatches = buildAllPairings(allTeams);
//        }while(allTeams.stream().anyMatch(team -> team.getHomeGamesCount()< 7 || team.getHomeGamesCount() >8 ));
        Map<Integer, List<Match>> seasonSchedule;

        //basically - keep trying till it works. locally, it took < ~10 seconds to generate a full schedule.
        //the retry is mostly caused by the rng trying to work with the scheduling constraints.
        do{
            seasonSchedule = WeekScheduler.buildWeeklySchedule(mainSeasonMatches);
        } while(seasonSchedule.entrySet().stream().anyMatch(week -> week.getValue().size() < 8));

        allTeams.sort(Comparator.comparing(team -> team.getDivision().ordinal()));
        System.out.println("Schedule by team");
//        allTeams.forEach(team -> System.out.println(team.printSchedule()));
        System.out.println("North Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.NORTH_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("North Kai teams with 7 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.NORTH_KAI && team.getHomeGamesCount()==7).count());
        System.out.println("");
        System.out.println("East Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.EAST_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("East Kai teams with 7 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.EAST_KAI && team.getHomeGamesCount()==7).count());
        System.out.println("");
        System.out.println("West Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.WEST_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("West Kai teams with 7 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.WEST_KAI && team.getHomeGamesCount()==7).count());
        System.out.println("");
        System.out.println("South Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.SOUTH_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("South Kai teams with 7 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.SOUTH_KAI && team.getHomeGamesCount()==7).count());


        System.out.println("Schedule by week");
        StringBuilder builder = new StringBuilder();
        allTeams.forEach(team -> builder.append(team.printSchedule()));

        seasonSchedule.forEach((week, matches) -> {
            builder.append("Week ").append(week).append('\n');
            matches.forEach(match -> builder.append(match.getMatchDescription()).append(", "));
            builder.append('\n');
        });
        try{
            Files.write(Paths.get("/home/jordan/schedule.txt"), builder.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        } catch(Exception e){
            System.out.println("unable to write to file: " + e.getLocalizedMessage());
        }

    }

    public static List<Match> buildAllPairings(List<Team> allTeams){
        List<Match> mainSeasonMatches = new ArrayList<>();
        for(Team team : allTeams){
            List<Team> remTeams = new ArrayList<>(allTeams).stream().filter(remTeam -> !remTeam.hasFoughtTeam(team.getName())).collect(Collectors.toList());
//            Collections.shuffle(remTeams);
            for(Team opponent : remTeams){
                if(team.getName().equalsIgnoreCase(opponent.getName())){
                    continue;
                }
                boolean homeGame = HomeGameProcessor.isHomeGame(team, opponent);
                Match match;
                if(homeGame){
                    match = new Match(team, 0, opponent);
                } else{
                    match = new Match(opponent, 0, team);
                }
                team.addMatchToSchedule(match);
                opponent.addMatchToSchedule(match);
                mainSeasonMatches.add(match);
            }

        }

        HomeGameProcessor.postProcessHomeGames(allTeams, mainSeasonMatches);
        return mainSeasonMatches;
    }


}
