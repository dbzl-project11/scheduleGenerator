package org.dbzl.schedule;

import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MatchBuilder {

    public static List<Match> buildAllPairings(List<Team> allTeams){
        List<Match> mainSeasonMatches = new ArrayList<>();
        for(Team team : allTeams){
            List<Team> remTeams = new ArrayList<>(allTeams).stream().filter(remTeam -> !remTeam.hasFoughtTeam(team.getName())).collect(Collectors.toList());
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
