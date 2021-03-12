package org.dbzl.schedule;

import java.util.*;
import java.util.function.Predicate;
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
//        }while(allTeams.stream().anyMatch(team -> team.getHomeGames()< 7 || team.getHomeGames() >8 ));
        Map<Integer, List<Match>> seasonSchedule;

        //basically - keep trying till it works. locally, it took < ~10 seconds to generate a full schedule.
        //the retry is mostly caused by the rng trying to work with the scheduling constraints.
        do{
            seasonSchedule = buildWeeklySchedule(mainSeasonMatches);
        } while(seasonSchedule.entrySet().stream().anyMatch(week -> week.getValue().size() < 8));

        allTeams.sort(Comparator.comparing(team -> team.getDivision().ordinal()));
        System.out.println("Schedule by team");
        allTeams.forEach(team -> System.out.println(team.printSchedule()));
        System.out.println("North Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.NORTH_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("East Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.EAST_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("West Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.WEST_KAI && team.getHomeGamesCount()==8).count());
        System.out.println("South Kai teams with 8 home games: " + allTeams.stream().filter(team -> team.getDivision()== Division.SOUTH_KAI && team.getHomeGamesCount()==8).count());


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
//            Collections.shuffle(remTeams);
            for(Team opponent : remTeams){
                if(team.getName().equalsIgnoreCase(opponent.getName())){
                    continue;
                }
                boolean homeGame = isHomeGame(team, opponent);
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

        postProcessHomeGames(allTeams, mainSeasonMatches);
        return mainSeasonMatches;
    }

    public static boolean isHomeGame(Team team, Team opponent){
        long team1HomeGames = team.getHomeGamesCount();
        long team2HomeGames = opponent.getHomeGamesCount();

        boolean team1CanHaveHomeGame = team1HomeGames < 8;
        boolean team2CanHaveHomeGame = team2HomeGames < 8;

        //handle divisional checks
        if(team.getDivision() == opponent.getDivision()){
            long team1DivisionalHomeGames = team.getDivisionalHomeGamesCount();
            long team2DivisionalHomeGames = opponent.getDivisionalHomeGamesCount();

            if(team1CanHaveHomeGame && team2CanHaveHomeGame) {

                if(team1DivisionalHomeGames == team2DivisionalHomeGames){
                    return rng.nextBoolean();
                } else if(team2DivisionalHomeGames ==2){
                    return true;
                } else if(team1DivisionalHomeGames ==2){
                    return false;
                }

                return team1DivisionalHomeGames < team2DivisionalHomeGames;//prefer whoever has the fewest divisional home games
            }
            //prioritize having 7 or 8 home games above having home map for divisional matches
            if(team1CanHaveHomeGame){
                return true;
            } else if(team2CanHaveHomeGame){
                return false;
            }
            return team1DivisionalHomeGames < team2DivisionalHomeGames;
        }


        //check if a team has 8+ home games
        if(team1CanHaveHomeGame && !team2CanHaveHomeGame){
            return true;
        } else if(team2CanHaveHomeGame && !team1CanHaveHomeGame){
            return false;
        }

        //both have less than 8 home games
        if(team1CanHaveHomeGame && team2CanHaveHomeGame) {
            //check how many matches are left to be scheduled, and how many max possible home games can be scheduled
            long remainingGamesTeam1 = 15 - team.getSchedule().size();
            long remainingGamesTeam2 = 15 - opponent.getSchedule().size();

            long remainingPossibleHomeGamesTeam1 = 8 - team1HomeGames;
            long remainingPossibleHomeGamesTeam2 = 8 - team2HomeGames;

            //in the last stretch of the season, is this team in danger of not getting to 7/8 home games
            boolean team1NeedsPriority = remainingGamesTeam1 <= remainingPossibleHomeGamesTeam1;
            boolean team2NeedsPriority = remainingGamesTeam2 <= remainingPossibleHomeGamesTeam2;

            if (team1NeedsPriority && team2NeedsPriority) {
                System.out.println("Both " + team.getName() + " and " + opponent.getName() + " need priority");
                if (remainingPossibleHomeGamesTeam1 == remainingPossibleHomeGamesTeam2) {
                    System.out.println("teams need priority");

                    return rng.nextBoolean();
                }
                return remainingPossibleHomeGamesTeam1 > remainingPossibleHomeGamesTeam2; //prioritize the team who needs more home games
            } else if (team1NeedsPriority) {
                return true;
            } else if (team2NeedsPriority) {
                return false;
            }
            //check non-priority situations
            if (team1HomeGames < team2HomeGames || team2HomeGames < team1HomeGames) {
                return team1HomeGames < team2HomeGames; // both under 8, but 1 is closer to their limit
            }
            return rng.nextBoolean();//both teams under 8 home games, so it doesn't really matter who gets the home game
        }
        if(team1HomeGames== team2HomeGames){
            return rng.nextBoolean();
        }
        return team1HomeGames < team2HomeGames; //if all else fails, prefer the team with the fewest home games
    }

    public static void postProcessHomeGames(List<Team> allTeams, List<Match> mainSeasonMatches){
        List<Team> teams = allTeams.stream().filter(team -> team.getHomeGamesCount()< 7 || team.getHomeGamesCount() >=8).collect(Collectors.toList());
        teams.sort(Comparator.comparingLong(Team::getHomeGamesCount));
        for(Team team: teams){
            //first make sure no one has < 7 home games
            if(team.getHomeGamesCount() < 7){
                List<Match> awayGames = team.getSchedule().stream().filter(match -> !match.getHomeTeam().getName().equalsIgnoreCase(team.getName())).collect(Collectors.toList());//don't remove
                for(Match game : awayGames){
                    if(game.isDivisionalMatch() || game.getHomeTeam().getDivisionalHomeGamesCount() >=8){
                        continue; //don't mess with divisional matches or teams with < 8 home games
                    }

                    Team oldHomeTeam = game.getHomeTeam();
                    oldHomeTeam.getSchedule().remove(game);
                    team.getSchedule().remove(game);
                    Match newMatch = new Match(team, game.getWeek(), oldHomeTeam);
                    oldHomeTeam.addMatchToSchedule(newMatch);
                    team.addMatchToSchedule(newMatch);
                    mainSeasonMatches.remove(game);
                    mainSeasonMatches.add(newMatch);
                    if(team.getHomeGamesCount() >= 7){
                        break; //once we get them to the minimum, don't look at any further matches;
                    }
                }
            }
        }

        for(Division kai : Division.values()){
            long count8Home = teams.stream().filter(team -> team.getDivision()== kai && team.getHomeGamesCount()==8).count();
            long count7Home = teams.stream().filter(team -> team.getDivision()== kai && team.getHomeGamesCount()==7).count();
            if(count8Home == count7Home){
                continue; // should only happen if there are 2 teams with 8 home games and 2 teams with 7 home games
            }
            if(count8Home > 2){
                List<Team> homeTeams8 = teams.stream().filter(team -> team.getDivision()== kai && team.getHomeGamesCount()==8).collect(Collectors.toList());
                for(Team home : homeTeams8){
                    List<Team> eligibleSwapTeams = teams.stream().filter(team -> team.getDivision()!= kai && team.getHomeGamesCount()==7).collect(Collectors.toList());
                    for(Team awayTeam : eligibleSwapTeams){
                        //first check to see if we'd make the other div worse
                        long otherKai8HomeCount = teams.stream().filter(team -> team.getDivision()== awayTeam.getDivision() && team.getHomeGamesCount()==8).count();
                        if(otherKai8HomeCount < 2){
                            Match toReplace = home.getSchedule().stream().filter(match -> match.getHomeTeam().equals(home) && match.getAwayTeam().equals(awayTeam)).findAny().get();
                            Match replacement = new Match(awayTeam, toReplace.getWeek(), home);
                            home.getSchedule().remove(toReplace);
                            awayTeam.getSchedule().remove(toReplace);
                            home.addMatchToSchedule(replacement);
                            awayTeam.addMatchToSchedule(replacement);
                            mainSeasonMatches.remove(toReplace);
                            mainSeasonMatches.add(replacement);
                        } else if(otherKai8HomeCount >=2){
                            continue;
                        }
                    }
                }
            }

        }



    }

}
