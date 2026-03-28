package org.dbzl.schedule;

import org.dbzl.domain.Division;
import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.util.*;
import java.util.stream.Collectors;

public class HomeGameProcessor {

    private static final Random rng = new Random();

    public static boolean isHomeGame(Team team, Team opponent, int totalTeams){
        int upperBound = totalTeams / 2;
        int lowerBound = upperBound - 1;
        int weeksInSeason = totalTeams - 1;

        long team1HomeGames = team.getHomeGamesCount();
        long team2HomeGames = opponent.getHomeGamesCount();

        // Divisional specific logic: prioritize balancing divisional home games
        if (team.getDivision() == opponent.getDivision()) {
            long team1DivisionalHome = team.getDivisionalHomeGamesCount();
            long team2DivisionalHome = opponent.getDivisionalHomeGamesCount();
            if (team1DivisionalHome != team2DivisionalHome) {
                return team1DivisionalHome < team2DivisionalHome;
            }
        }

        boolean team1CanHaveHomeGame = team1HomeGames < upperBound;
        boolean team2CanHaveHomeGame = team2HomeGames < lowerBound;

        //check if a team is already at their limit
        if(team1CanHaveHomeGame && !team2CanHaveHomeGame){
            return true;
        } else if(team2CanHaveHomeGame && !team1CanHaveHomeGame){
            return false;
        }

        //both have room for home games
        if(team1CanHaveHomeGame && team2CanHaveHomeGame) {
            //check how many matches are left to be scheduled, and how many max possible home games can be scheduled
            long remainingGamesTeam1 = weeksInSeason - team.getSchedule().size();
            long remainingGamesTeam2 = weeksInSeason - opponent.getSchedule().size();

            long remainingPossibleHomeGamesTeam1 = upperBound - team1HomeGames;
            long remainingPossibleHomeGamesTeam2 = upperBound - team2HomeGames;

            //in the last stretch of the season, is this team in danger of not getting to minimum home games
            boolean team1NeedsPriority = remainingGamesTeam1 <= remainingPossibleHomeGamesTeam1;
            boolean team2NeedsPriority = remainingGamesTeam2 <= remainingPossibleHomeGamesTeam2;

            if (team1NeedsPriority && team2NeedsPriority) {
                if (remainingPossibleHomeGamesTeam1 == remainingPossibleHomeGamesTeam2) {
                    return rng.nextBoolean();
                }
                return remainingPossibleHomeGamesTeam1 > remainingPossibleHomeGamesTeam2; //prioritize the team who needs more home games
            } else if (team1NeedsPriority) {
                return true;
            } else if (team2NeedsPriority) {
                return false;
            }
            //check non-priority situations
            if (team1HomeGames != team2HomeGames) {
                return team1HomeGames < team2HomeGames; // both under limit, but prioritize the one with fewer home games
            }
            return rng.nextBoolean();
        }
        if(team1HomeGames == team2HomeGames){
            return rng.nextBoolean();
        }
        return team1HomeGames < team2HomeGames; //if all else fails, prefer the team with the fewest home games
    }

    public static void postProcessHomeGames(List<Team> allTeams, List<Match> mainSeasonMatches){
        int totalTeams = allTeams.size();
        int upperBound = totalTeams / 2;
        int lowerBound = upperBound - 1;

        // 1. First ensure every team has at least one divisional home game and not all divisional home games
        ensureDivisionalHomeGameConstraints(allTeams, mainSeasonMatches);

        // 2. Proceed with general balance
        postProcessDivisionals(allTeams, mainSeasonMatches, upperBound, lowerBound);
        postProcessNormals(allTeams, mainSeasonMatches, upperBound, lowerBound);
        postProcessHomeGamesByDivision(allTeams, mainSeasonMatches, upperBound, lowerBound);
    }

    private static void ensureDivisionalHomeGameConstraints(List<Team> allTeams, List<Match> mainSeasonMatches) {
        Map<Division, List<Team>> teamsByDivision = allTeams.stream().collect(Collectors.groupingBy(Team::getDivision));

        for (List<Team> divisionTeams : teamsByDivision.values()) {
            int maxPossibleDivisionalHome = divisionTeams.size() - 1;
            if (maxPossibleDivisionalHome <= 0) continue;

            for (Team team : divisionTeams) {
                long divisionalHomeCount = team.getDivisionalHomeGamesCount();

                // Ensure at least 1 home game
                if (divisionalHomeCount == 0) {
                    Match divisionalAway = team.getSchedule().stream()
                            .filter(Match::isDivisionalMatch)
                            .filter(m -> m.getAwayTeam().equals(team))
                            .findFirst().orElse(null);
                    if (divisionalAway != null) {
                        updateMatches(divisionalAway, team, divisionalAway.getHomeTeam(), mainSeasonMatches);
                    }
                }

                // Re-check count after potential fix above
                divisionalHomeCount = team.getDivisionalHomeGamesCount();

                // Ensure not all home games
                if (divisionalHomeCount == maxPossibleDivisionalHome && maxPossibleDivisionalHome > 1) {
                    Match divisionalHome = team.getSchedule().stream()
                            .filter(Match::isDivisionalMatch)
                            .filter(m -> m.getHomeTeam().equals(team))
                            .findFirst().orElse(null);
                    if (divisionalHome != null) {
                        updateMatches(divisionalHome, divisionalHome.getAwayTeam(), team, mainSeasonMatches);
                    }
                }
            }
        }
    }

    private static boolean canFlipMatch(Match match, Team teamToReceiveHome, Team teamToLoseHome) {
        if (!match.isDivisionalMatch()) {
            return true;
        }
        
        // If it's a divisional match, we must ensure we don't violate the "at least 1, not all" rule
        long teamToReceiveDivHomeCount = teamToReceiveHome.getDivisionalHomeGamesCount();
        long teamToLoseDivHomeCount = teamToLoseHome.getDivisionalHomeGamesCount();
        
        // We assume division sizes are consistent within the match
        // Need to know division size to check "not all"
        // This is a bit tricky without passing the division size or recalculating it.
        // But we can check if it's currently at 1 and we're taking it away.
        if (teamToLoseDivHomeCount <= 1) {
            return false; // Can't take away the last divisional home game
        }
        
        // For "not all", we can check how many divisional games they have in total
        long totalDivGames = teamToReceiveHome.getSchedule().stream().filter(Match::isDivisionalMatch).count();
        if (teamToReceiveDivHomeCount + 1 >= totalDivGames && totalDivGames > 1) {
            return false; // Can't give all divisional home games
        }
        
        return true;
    }

    public static void postProcessNormals(List<Team> allTeams, List<Match> mainSeasonMatches, int upperBound, int lowerBound){
        List<Team> teamsWithTooFewHomeGames = allTeams.stream().filter(team -> team.getHomeGamesCount() < lowerBound).toList();
        List<Team> teamsWithTooManyHomeGames = allTeams.stream().filter(team -> team.getHomeGamesCount() > upperBound).toList();

        if(teamsWithTooManyHomeGames.isEmpty() && teamsWithTooFewHomeGames.isEmpty()){
            return;
        }

        for(Team team : teamsWithTooManyHomeGames){
            for(Team opponent : teamsWithTooFewHomeGames){
                if(team.getHomeGamesCount() <= upperBound || opponent.getHomeGamesCount() >= lowerBound){
                    break;
                }

                Match currentMatch = team.getSchedule().stream().filter(match -> match.getHomeTeam().equals(opponent) ||
                        match.getAwayTeam().equals(opponent)).findFirst( ).orElse(null);
                
                if(currentMatch != null && currentMatch.getHomeTeam().equals(team)){
                    if (canFlipMatch(currentMatch, opponent, team)) {
                        updateMatches(currentMatch, team, opponent, mainSeasonMatches);
                    }
                }
            }
        }
    }

    public static void postProcessDivisionals(List<Team> allTeams, List<Match> mainSeasonMatches, int upperBound, int lowerBound){
        List<Team> teams = allTeams.stream().filter(team -> team.getHomeGamesCount() < lowerBound || team.getHomeGamesCount() > upperBound).
                sorted(Comparator.comparingLong(Team::getHomeGamesCount)).toList();

        for (Team team : teams) {
                if (team.getHomeGamesCount() < lowerBound) {
                    List<Match> awayGames = team.getSchedule().stream().filter(match -> !match.getHomeTeam().equals(team)).toList();
                    for (Match game : awayGames) {
                        if (game.getAwayTeam().getHomeGamesCount() <= lowerBound) {
                            continue;
                        }

                        if (canFlipMatch(game, team, game.getHomeTeam())) {
                            Team oldHomeTeam = game.getHomeTeam();
                            updateMatches(game, oldHomeTeam, team, mainSeasonMatches);
                        }

                        if (team.getHomeGamesCount() >= lowerBound) {
                            break;
                        }
                    }
                } else if (team.getHomeGamesCount() > upperBound) {
                    List<Match> homeGames = team.getSchedule().stream().filter(match -> match.getHomeTeam().equals(team)).toList();
                    for (Match game : homeGames) {
                        if (game.getAwayTeam().getHomeGamesCount() >= upperBound) {
                            continue;
                        }
                        
                        if (canFlipMatch(game, game.getAwayTeam(), team)) {
                            Team oldHomeTeam = game.getHomeTeam();
                            updateMatches(game, oldHomeTeam, team, mainSeasonMatches);
                        }

                       if (team.getHomeGamesCount() <= upperBound) {
                           break;
                       }
                    }
                }
            }
    }


    public static void postProcessHomeGamesByDivision(List<Team> allTeams, List<Match> mainSeasonMatches, int upperBound, int lowerBound){
        List<Division> divisions = List.of(Division.values());
        boolean divisionsNeedTuning = true;
        
        while(divisionsNeedTuning) {
            for (Division kai : divisions) {
                long countUpperHome = countTeamsWithXHomeGames(allTeams, upperBound, kai);
                long countLowerHome = countTeamsWithXHomeGames(allTeams, lowerBound, kai);
                
                if (countUpperHome > 0 && countLowerHome > 0) {
                    continue;
                }
                
                List<Team> teamsInDivision = allTeams.stream().filter(team -> team.getDivision() == kai).toList();
                
                if (countLowerHome == 0) {
                    for (Team teamWithUpperHomeGames : teamsInDivision) {
                        if (teamWithUpperHomeGames.getHomeGamesCount() < upperBound) continue;
                        
                        for (Division kai2 : divisions) {
                            if (teamWithUpperHomeGames.getHomeGamesCount() < upperBound) break;
                            
                            List<Team> eligibleTeams = allTeams.stream().filter(team -> team.getDivision() == kai2 && team.getHomeGamesCount() <= lowerBound).toList();
                            processLowerBoundTeams(mainSeasonMatches, teamWithUpperHomeGames, eligibleTeams, lowerBound);
                        }
                    }
                } else if (countUpperHome == 0) {
                    for (Team teamWithLowerHomeGames : teamsInDivision) {
                        if (teamWithLowerHomeGames.getHomeGamesCount() > lowerBound) continue;
                        
                        for (Division kai2 : divisions) {
                            if (teamWithLowerHomeGames.getHomeGamesCount() > lowerBound) break;
                            
                            List<Team> eligibleTeams = allTeams.stream().filter(team -> team.getDivision() == kai2 && team.getHomeGamesCount() >= upperBound).toList();
                            processUpperBoundTeams(mainSeasonMatches, teamWithLowerHomeGames, eligibleTeams, upperBound);
                        }
                    }
                }
            }

            divisionsNeedTuning = allTeams.stream().anyMatch(team -> team.getHomeGamesCount() > upperBound || team.getHomeGamesCount() < lowerBound);
        }
    }

    private static void processUpperBoundTeams(List<Match> mainSeasonMatches, Team teamWithLowerHomeGames, List<Team> eligibleTeams, int upperBound) {
        for (Team teamWithUpperHomeGames : eligibleTeams) {
            if (teamWithUpperHomeGames.getHomeGamesCount() <= upperBound) {
                continue;
            }
            Optional<Match> oldMatchOpt = teamWithUpperHomeGames.getSchedule().stream().filter(match -> match.getHomeTeam().equals(teamWithUpperHomeGames) && match.getAwayTeam().equals(teamWithLowerHomeGames)).findFirst();
            if (oldMatchOpt.isPresent()) {
                Match oldMatch = oldMatchOpt.get();
                if (canFlipMatch(oldMatch, teamWithLowerHomeGames, teamWithUpperHomeGames)) {
                    updateMatches(oldMatch, teamWithLowerHomeGames, teamWithUpperHomeGames, mainSeasonMatches);
                }
                if (teamWithLowerHomeGames.getHomeGamesCount() >= upperBound) {
                    break;
                }
            }
        }
    }

    private static void processLowerBoundTeams(List<Match> mainSeasonMatches, Team teamWithUpperHomeGames, List<Team> eligibleTeams, int lowerBound) {
        for (Team teamWithLowerHomeGames : eligibleTeams) {
            if (teamWithLowerHomeGames.getHomeGamesCount() >= lowerBound) {
                continue;
            }
            Optional<Match> oldMatchOpt = teamWithUpperHomeGames.getSchedule().stream().filter(match -> match.getHomeTeam().equals(teamWithUpperHomeGames) && match.getAwayTeam().equals(teamWithLowerHomeGames)).findFirst();
            if (oldMatchOpt.isPresent()) {
                Match oldMatch = oldMatchOpt.get();
                if (canFlipMatch(oldMatch, teamWithLowerHomeGames, teamWithUpperHomeGames)) {
                    updateMatches(oldMatch, teamWithLowerHomeGames, teamWithUpperHomeGames, mainSeasonMatches);
                }
                if (teamWithUpperHomeGames.getHomeGamesCount() <= lowerBound) {
                    break;
                }
            }
        }
    }

    static long countTeamsWithXHomeGames(List<Team> teams, int homeGameCount, Division kai){
        return teams.stream().filter(team -> team.getDivision() == kai && team.getHomeGamesCount() == homeGameCount).count();
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
