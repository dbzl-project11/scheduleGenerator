package org.dbzl.schedule;

import org.dbzl.domain.Division;
import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.util.*;
import java.util.stream.Collectors;

public class HomeGameProcessor {

    private static final Random rng = new Random();


    public static boolean isHomeGame(Team team, Team opponent){
        long team1HomeGames = team.getHomeGamesCount();
        long team2HomeGames = opponent.getHomeGamesCount();

        boolean team1CanHaveHomeGame = team1HomeGames < 8;
        boolean team2CanHaveHomeGame = team2HomeGames < 8;

        //handle divisional checks
        if(team.getDivision() == opponent.getDivision()){
            return calculateDivisionalHomeTeam(team, opponent, team1CanHaveHomeGame, team2CanHaveHomeGame);
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

    private static boolean calculateDivisionalHomeTeam(Team team, Team opponent, boolean team1CanHaveHomeGame, boolean team2CanHaveHomeGame) {
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

            return team1DivisionalHomeGames < team2DivisionalHomeGames;
        }
        //prioritize having 7 or 8 home games above having home map for divisional matches
        if(team1CanHaveHomeGame){
            return true;
        } else if(team2CanHaveHomeGame){
            return false;
        }
        return team1DivisionalHomeGames < team2DivisionalHomeGames;
    }

    public static void postProcessHomeGames(List<Team> allTeams, List<Match> mainSeasonMatches){
        postProcessDivisionals(allTeams, mainSeasonMatches);
        postProcessNormals(allTeams, mainSeasonMatches);
        postProcessHomeGamesByDivision(allTeams, mainSeasonMatches);

    }

    public static void postProcessNormals(List<Team> allTeams, List<Match> mainSeasonMatches){
        List<Team> teamsWithTooFewHomeGames = allTeams.stream().filter(team -> team.getHomeGamesCount() < 7).toList();
        List<Team> teamsWithTooManyHomeGames = allTeams.stream().filter(team -> team.getHomeGamesCount() > 8).toList();

        if(teamsWithTooManyHomeGames.isEmpty() && teamsWithTooFewHomeGames.isEmpty()){
            return; // in this case, no post-processing at this step because everyone is already at 7 or 8 home games.
        }

        //step 1; start by checking the matches between the teams in the 2 lists
        //ie if Androids have 9 home games and Cold has 6, and androids vs cold is home for Androids, then making it home for Cold is better
        for(Team team : teamsWithTooManyHomeGames){

            for(Team opponent : teamsWithTooFewHomeGames){
                if(team.getHomeGamesCount() == 7 || team.getHomeGamesCount() == 8){
                    break; // check before making any more changes because these teams are in flux;
                }
                 if(opponent.getHomeGamesCount() == 7 || opponent.getHomeGamesCount() == 8){
                    break; // check before making any more changes because these teams are in flux;
                }

                Match currentMatch = team.getSchedule().stream().filter(match -> match.getHomeTeam().equals(opponent) ||
                        match.getAwayTeam().equals(opponent)).findFirst( ).orElseThrow(() ->new IllegalStateException("Can't find valid match"));
                if(currentMatch.getHomeTeam().equals(team)){
                    updateMatches(currentMatch, team, opponent, mainSeasonMatches);
                }
            }

        }
    }

    public static void postProcessDivisionals(List<Team> allTeams, List<Match> mainSeasonMatches){
        List<Team> teams = allTeams.stream().filter(team -> team.getHomeGamesCount() < 7 || team.getHomeGamesCount() >= 8).
                sorted(Comparator.comparingLong(Team::getHomeGamesCount)).toList();

        for (Team team : teams) {
                //first make sure no one has < 7 home games
                if (team.getHomeGamesCount() < 7) {
                    List<Match> awayGames = team.getSchedule().stream().filter(match -> !match.getHomeTeam().equals(team)).toList();//don't remove
                    for (Match game : awayGames) {
                        if (game.isDivisionalMatch() || game.getAwayTeam().getHomeGamesCount() >= 8) {
                            continue; //don't mess with divisional matches or teams with > 8 home games
                        }

                        Team oldHomeTeam = game.getHomeTeam();
                        updateMatches(game, oldHomeTeam, team, mainSeasonMatches);

                        if (team.getHomeGamesCount() >= 7) {
                            break; //once we get them to the minimum, don't look at any further matches;
                        }
                    }
                } else if (team.getHomeGamesCount() > 8) {

                    List<Match> homeGames = team.getSchedule().stream().filter(match -> match.getHomeTeam().equals(team)).toList();//don't remove
                    for (Match game : homeGames) {
                        if (game.isDivisionalMatch() || game.getAwayTeam().getHomeGamesCount() < 7) {
                            continue; //don't mess with divisional matches or teams with < 8 home games
                        }
                        Team oldHomeTeam = game.getHomeTeam();

                       updateMatches(game,  oldHomeTeam, team, mainSeasonMatches);

                       if (team.getHomeGamesCount() <= 8) {
                           break; //once we get them to the max, don't look at any further matches;
                       }
                    }
                }
            }
    }


    public static void postProcessHomeGamesByDivision(List<Team> allTeams, List<Match> mainSeasonMatches){
        List<Division> divisions = List.of(Division.values());
        boolean divisionsNeedTuning = true;
        System.out.println("starting home game post-processing by division");
        while(divisionsNeedTuning) {

            for (Division kai : divisions) {
                long count8Home = countTeamsWithXHomeGames(allTeams, 8, kai);
                long count7Home = countTeamsWithXHomeGames(allTeams, 7, kai);
                    if (count8Home == count7Home) {
                        //ignore a division at 2 & 2
                        continue;
                    } else{
                        System.out.println(kai + " has " + count7Home + " with 7 home games and " + count8Home + " teams with 8 home games");
                    }
                List<Team> teamsWith8Games = allTeams.stream().filter(team -> team.getDivision() == kai && team.getHomeGamesCount() >= 8).toList();
                for (Team teamWith8HomeGames : teamsWith8Games) {
                    count8Home = countTeamsWithXHomeGames(allTeams, 8, kai);
                    count7Home = countTeamsWithXHomeGames(allTeams, 7, kai);
                    if (count8Home == count7Home) {
                         //ignore a division at 2 & 2
                        break;
                    }
                    for (Division kai2 : divisions) {

                        if (teamWith8HomeGames.getHomeGamesCount() < 8) {
                            break;
                        }
                        List<Team> eligibleTeams = allTeams.stream().filter(team -> team.getDivision() == kai2 && team.getHomeGamesCount() < 8).toList();
                        count8Home = countTeamsWithXHomeGames(allTeams, 8, kai2);
                        count7Home = countTeamsWithXHomeGames(allTeams, 7, kai2);
                        if (count8Home == count7Home) {
                            System.out.println("Excluding: " + kai2 + " because it's already been equalized");
                            continue; //exclude divisions that are at 2 & 2
                        }
                        if(count8Home > 2 && (kai != kai2)){
                            continue; //can't move another home game into this division; only if necessary will this method change divisional matches

                        }
                        process7Count(mainSeasonMatches, teamWith8HomeGames, eligibleTeams);
                    }
                }

                List<Team> teamsWith7Games = allTeams.stream().filter(team -> team.getDivision() == kai && team.getHomeGamesCount() < 8).toList();
                for(Team teamwith7Games : teamsWith7Games){
                    count8Home = countTeamsWithXHomeGames(allTeams, 8, kai);
                    count7Home = countTeamsWithXHomeGames(allTeams, 7, kai);
                    if (count8Home == count7Home) {
                         //ignore a division at 2 & 2
                        break;
                    }
                    for (Division kai2 : divisions) {

                        if (teamwith7Games.getHomeGamesCount() > 7) {
                            break;
                        }
                        List<Team> teamsWithMoreHomeGames = allTeams.stream().filter(team -> team.getDivision() == kai2 && team.getHomeGamesCount() > 7).collect(Collectors.toList());
                        count8Home = countTeamsWithXHomeGames(allTeams, 8, kai2);
                        count7Home = countTeamsWithXHomeGames(allTeams, 7, kai2);
                        if (count8Home == count7Home) {
                            System.out.println("Excluding: " + kai2 + " because it's already been equalized");
                            continue; //exclude divisions that are at 2 & 2
                        }
                        if(count8Home > 2 && (kai != kai2)){
                            continue; //can't move another home game into this division; only if necessary will this method change divisional matches

                        }
                        process8Count(mainSeasonMatches, teamwith7Games, teamsWithMoreHomeGames);
                    }
                }
            }
            int finishedDivisions = 0;
            for(Division kai : divisions){
                long count8Home = countTeamsWithXHomeGames(allTeams, 8, kai);
                long count7Home = countTeamsWithXHomeGames(allTeams, 7, kai);
                if (count8Home == count7Home) {
                        finishedDivisions++;
                }
            }
            if(finishedDivisions == 4){
                System.out.println("ALL DIVISIONS EQUAL");
                divisionsNeedTuning = false;
            }
        }
//        }
    }

    private static void process8Count(List<Match> mainSeasonMatches, Team teamwith7Games, List<Team> eligibleTeams) {
        for (Team teamWith8HomeGames : eligibleTeams) {

            if (teamWith8HomeGames.getHomeGamesCount() > 7) {
                continue; //shouldn't happen, but just in case . . .
            }
            boolean matchIsHomeGame = teamWith8HomeGames.getSchedule().stream().anyMatch(match -> match.getAwayTeam().equals(teamWith8HomeGames));
            if (matchIsHomeGame) {
                Match oldMatch = teamWith8HomeGames.getSchedule().stream().filter(match -> match.getAwayTeam().equals(teamWith8HomeGames)).findFirst().get();
                System.out.println("Flipping : " + oldMatch.getMatchDescription());

                updateMatches(oldMatch, teamwith7Games, teamWith8HomeGames, mainSeasonMatches);
                if (teamwith7Games.getHomeGamesCount() > 7) {
                    break;
                }
            }
        }
    }

    private static void process7Count(List<Match> mainSeasonMatches, Team teamWith8HomeGames, List<Team> eligibleTeams) {
        for (Team teamWith7HomeGames : eligibleTeams) {

            if (teamWith7HomeGames.getHomeGamesCount() > 7) {
                continue; //shouldn't happen, but just in case . . .
            }
            boolean matchIsHomeGame = teamWith8HomeGames.getSchedule().stream().anyMatch(match -> match.getAwayTeam().equals(teamWith7HomeGames));
            if (matchIsHomeGame) {
                Match oldMatch = teamWith8HomeGames.getSchedule().stream().filter(match -> match.getAwayTeam().equals(teamWith7HomeGames)).findFirst().get();
                System.out.println("Flipping : " + oldMatch.getMatchDescription());

                updateMatches(oldMatch, teamWith7HomeGames, teamWith8HomeGames, mainSeasonMatches);
                if (teamWith8HomeGames.getHomeGamesCount() < 8) {
                    break;
                }
            }
        }
    }

    static long countTeamsWithXHomeGames(List<Team> teams, int HomeGameCount, Division kai){
        return teams.stream().filter(team -> team.getDivision() == kai && team.getHomeGamesCount() == HomeGameCount).count();
    }

    public static void updateMatches(Match oldMatch, Team homeTeam, Team awayTeam, List<Match> mainSeasonMatches){
        Match replacementMatch = oldMatch.flipHomeAndAway();
        homeTeam.getSchedule().remove(oldMatch);
        awayTeam.getSchedule().remove(oldMatch);
        homeTeam.addMatchToSchedule(replacementMatch);
        awayTeam.addMatchToSchedule(replacementMatch);
        mainSeasonMatches.remove(oldMatch);
        mainSeasonMatches.add(replacementMatch);
    }

}
