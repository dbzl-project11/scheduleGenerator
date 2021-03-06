package org.dbzl.schedule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ScheduleGenerator {

    public static List<String> northKaiTeams = List.of("Buujins", "Budokai", "Androids", "Hybrids");
    public static List<String> eastKaiTeams = List.of("Namek", "GT", "Kaiju", "Earth Defenders");
    public static List<String> westKaiTeams = List.of("Royals", "Rugrats", "Cinema", "Resurrected Warriors");
    public static List<String> southKaiTeams = List.of("Cold", "Muscle", "Sentai", "Derp");

    private static final Random rng = new Random();

    public static void main(String [] args){
        List<Team> northKai = new ArrayList<>();
        northKaiTeams.forEach(team -> northKai.add(new Team(team, Division.NORTH_KAI)));
        List<Team> eastKai = new ArrayList<>();
        eastKaiTeams.forEach(team -> eastKai.add(new Team(team, Division.EAST_KAI)));
        List<Team> westKai = new ArrayList<>();
        westKaiTeams.forEach(team -> westKai.add(new Team(team, Division.WEST_KAI)));
        List<Team> southKai = new ArrayList<>();
        southKaiTeams.forEach(team -> southKai.add(new Team(team, Division.SOUTH_KAI)));

        List<Team> allTeams = new ArrayList<>(northKai);
        allTeams.addAll(eastKai);
        allTeams.addAll(westKai);
        allTeams.addAll(southKai);

        List<Match> mainSeasonMatches = buildAllPairings(allTeams);
        Map<Integer, List<Match>> seasonSchedule;

        //basically - keep trying till it works.
        do{
            seasonSchedule = buildWeeklySchedule(mainSeasonMatches, allTeams);
        } while(seasonSchedule.entrySet().stream().anyMatch(week -> week.getValue().size() < 8));

        System.out.println("Schedule by team");

        for(Team team : allTeams){
            System.out.println(team.getName());
            for(Match divisionalMatch : team.getSchedule()){
                System.out.println(divisionalMatch.toString() );

            }
        }

        System.out.println("Schedule by week");
        for(Map.Entry<Integer, List<Match>> week : seasonSchedule.entrySet()){
            System.out.println("Week " + week.getKey() + " paired " + week.getValue().size() + " matches");
            for(Match match : week.getValue()){
                System.out.print(match.getMatchDescription() +", ");
            }
            System.out.println("");
        }


    }

    public static Map<Integer, List<Match>> buildWeeklySchedule(List<Match> mainSeasonMatches, List<Team> allTeams){
        final int [] weeks = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        //need to make a copy of the list so that we can call this method multiple times
        List<Match> mainSeasonMatchesIdem = new ArrayList<>(mainSeasonMatches);
        Map<Integer, List<Match>> mainSeasonSchedule = new HashMap<>();
        Predicate<Match> divisionalPredicate = match ->  match.isDivisionalMatch();

        //to make things a little easier, pair the divisionals first, as these have the most stringent constraints
        for(int week : weeks){
            List<Match> matches = new ArrayList<>();
            mainSeasonSchedule.put(week, matches); //create the placeholder list here so it doesn't have to be created later

            final Division kai = Division.isDivisionalWeekFor(week);
            if( kai == null){
                continue; //no divisionals for the week, so no matches to schedule
            }
            //should start with 6 divisional matches, pairing 2 each divisional week;
            List<Match> eligibleMatches = mainSeasonMatchesIdem.stream().filter(match -> divisionalPredicate.test(match) &&
                    match.getHomeTeam().getDivision() == kai).collect(Collectors.toList());
            if(eligibleMatches.size()==2){
                for(Match div : eligibleMatches){
                    div.setWeek(week);
                    matches.add(div);
                    mainSeasonMatchesIdem.remove(div);

                }
                continue;
            }
            List<Team> pairedTeams = new ArrayList<>();
            Match firstDiv = eligibleMatches.get(rng.nextInt(eligibleMatches.size()));
            firstDiv.setWeek(week);
            matches.add(firstDiv);
            pairedTeams.add(firstDiv.getHomeTeam());
            pairedTeams.add(firstDiv.getAwayTeam());

            eligibleMatches.remove(firstDiv);
            eligibleMatches.removeIf(match -> pairedTeams.contains(match.getAwayTeam()) || pairedTeams.contains(match.getHomeTeam()));
            assert eligibleMatches.size() == 1;
            Match otherDiv = eligibleMatches.get(0);
            otherDiv.setWeek(week);
            matches.add(otherDiv);
            mainSeasonMatchesIdem.remove(firstDiv);
            mainSeasonMatchesIdem.remove(otherDiv);
        }

        for(Map.Entry<Integer, List<Match>> mainSeasonWeeks : mainSeasonSchedule.entrySet()){
                int week = mainSeasonWeeks.getKey();
                List<Match> weeklyMatches = mainSeasonWeeks.getValue();
                System.out.println("Pairing matches for week: " + week);
                List<Team> pairedTeams = new ArrayList<>();
                if(weeklyMatches.size() > 0){
                    for(Match match : weeklyMatches){
                        pairedTeams.add(match.getHomeTeam());
                        pairedTeams.add(match.getAwayTeam());
                    }
                }
                int clearCount = 0;
                Predicate<Match> teamsAlreadyPaired = match -> !(pairedTeams.contains(match.getHomeTeam()) || pairedTeams.contains(match.getAwayTeam()));

                while(weeklyMatches.size() < 8){

                    List<Match> eligibleMatches = mainSeasonMatchesIdem.stream().filter(teamsAlreadyPaired).collect(Collectors.toList());
                    if(eligibleMatches.isEmpty() && weeklyMatches.size() < 8){
                        if(clearCount < 20000){ //set to 10000 because that's what eventually worked
                            clearCount++;
                            for(Match match : weeklyMatches){
                                if(!match.isDivisionalMatch()){
                                    match.setWeek(0);
                                    mainSeasonMatchesIdem.add(match);
                                }
                            }
                            weeklyMatches.removeIf(match -> !match.isDivisionalMatch());
                            System.out.println("unable to completely assign week, restarting week");
                            pairedTeams.clear();
                            for(Match match : weeklyMatches){
                                pairedTeams.add(match.getHomeTeam());
                                pairedTeams.add(match.getAwayTeam());
                            }
                            continue;
                        }
                        System.out.println("Giving up on assigning week and continuing");
                        List<Team> unpairedTeams = new ArrayList<>(allTeams);
                        unpairedTeams.removeAll(pairedTeams);
                        for(Team unpaired : unpairedTeams){
                            System.out.println(unpaired.getName());
                        }
                        break;
                    }
                    Match match = eligibleMatches.get(rng.nextInt(eligibleMatches.size()));

                    //cleanup after knowing the match should be assigned.
                    match.setWeek(week);
                    weeklyMatches.add(match);
                    mainSeasonMatchesIdem.remove(match);
                    pairedTeams.add(match.getHomeTeam());
                    pairedTeams.add(match.getAwayTeam());
                }
        }

        return mainSeasonSchedule;
    }

    public static List<Match> buildAllPairings(List<Team> allTeams){
        List<Match> mainSeasonMatches = new ArrayList<>();
        for(Team team : allTeams){
            List<Team> remTeams = new ArrayList<>(allTeams).stream().filter(remTeam -> !remTeam.hasFoughtTeam(team.getName())).collect(Collectors.toList());
            for(Team opponent : remTeams){
                if(team.getName().equalsIgnoreCase(opponent.getName())){
                    continue;
                }
                boolean homeGame = rng.nextInt() %2 ==0 && team.getHomeGames() < 8;
                Match match;
                if(homeGame){
                    match = new Match(team, 0, opponent);
                    team.addHomeGame();
                } else{
                    match = new Match(opponent, 0, team);
                    opponent.addHomeGame();
                }
                team.addMatchToSchedule(match);
                opponent.addMatchToSchedule(match);
                mainSeasonMatches.add(match);
            }

        }
        mainSeasonMatches.removeIf(match -> match.getHomeTeam().getName().equals(match.getAwayTeam().getName()));
        return mainSeasonMatches;
    }

}
