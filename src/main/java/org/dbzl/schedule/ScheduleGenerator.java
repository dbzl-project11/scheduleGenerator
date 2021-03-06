package org.dbzl.schedule;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ScheduleGenerator {

    //to adjust for new seasons, the only thing that *should* need to be done is rearrange the teams into the right Kais
    public static List<String> northKaiTeams = List.of("Buujins", "Budokai", "Androids", "Hybrids");
    public static List<String> eastKaiTeams = List.of("Namek", "GT", "Kaiju", "Earth Defenders");
    public static List<String> westKaiTeams = List.of("Royals", "Rugrats", "Cinema", "Resurrected Warriors");
    public static List<String> southKaiTeams = List.of("Cold", "Muscle", "Sentai", "Derp");

    private static final Random rng = new Random();

    public static void main(String [] args){

        List<Team> allTeams = northKaiTeams.stream().map(team -> new Team(team, Division.NORTH_KAI)).collect(Collectors.toList());
        allTeams.addAll(eastKaiTeams.stream().map(team -> new Team(team, Division.EAST_KAI)).collect(Collectors.toList()));
        allTeams.addAll( westKaiTeams.stream().map(team -> new Team(team, Division.WEST_KAI)).collect(Collectors.toList()));
        allTeams.addAll(southKaiTeams.stream().map(team -> new Team(team, Division.SOUTH_KAI)).collect(Collectors.toList()));

        List<Match> mainSeasonMatches = buildAllPairings(allTeams);
        Map<Integer, List<Match>> seasonSchedule;

        //basically - keep trying till it works. locally, it took < ~10 seconds to generate a full schedule.
        //the retry is mostly caused by the rng trying to work with the scheduling constraints.
        do{
            seasonSchedule = buildWeeklySchedule(mainSeasonMatches);
        } while(seasonSchedule.entrySet().stream().anyMatch(week -> week.getValue().size() < 8));

        System.out.println("Schedule by team");
        allTeams.forEach(team -> System.out.println(team.printSchedule()));

        System.out.println("Schedule by week");
        seasonSchedule.forEach((week, matches) -> {
            System.out.println("Week " + week);
            matches.forEach(match -> System.out.print(match.getMatchDescription() +", "));
            System.out.println();
        });

    }

    public static Map<Integer, List<Match>> buildWeeklySchedule(List<Match> mainSeasonMatches){
        final int [] weeks = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        //need to make a copy of the list so that we can call this method multiple times
        List<Match> mainSeasonMatchesIdem = new ArrayList<>(mainSeasonMatches);
        Map<Integer, List<Match>> mainSeasonSchedule = new HashMap<>();
        Predicate<Match> divisionalPredicate = Match::isDivisionalMatch;

        //to make things a little easier, pair the divisionals first, as these have the most stringent constraints
        for(int week : weeks){
            List<Match> matches = new ArrayList<>();
            mainSeasonSchedule.put(week, matches); //create the placeholder list here so it doesn't have to be created later

            for(Division kai : Division.values()){
                if(!kai.isDivisionalWeek(week)){
                    continue;
                }
                //first divisional week, each divisional will have 6 possible matches, then 4 (for the 2nd) then 2 in the last one)
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
                assert eligibleMatches.size() == 1; //sanity check;
                Match otherDiv = eligibleMatches.get(0);
                otherDiv.setWeek(week);
                matches.add(otherDiv);
                mainSeasonMatchesIdem.remove(firstDiv);
                mainSeasonMatchesIdem.remove(otherDiv);
            }
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
                        if(clearCount < 200){ //if a given iteration is too hard, just restart from scratch
                            clearCount++;
                            for(Match match : weeklyMatches){
                                if(!match.isDivisionalMatch()){
                                    match.setWeek(0);
                                    mainSeasonMatchesIdem.add(match);
                                }
                            }
                            weeklyMatches.removeIf(match -> !match.isDivisionalMatch());
//                            System.out.println("unable to completely assign week, restarting week");
                            pairedTeams.clear();
                            for(Match match : weeklyMatches){
                                pairedTeams.add(match.getHomeTeam());
                                pairedTeams.add(match.getAwayTeam());
                            }
                            continue;
                        }
                        System.out.println("Giving up on assigning week, reseeding");

                        return mainSeasonSchedule; //return because once it fails to assign a match, the safeguards above will catch it.
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
                } else{
                    match = new Match(opponent, 0, team);
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
