package controllers;

import java.util.HashMap;

import model.Lobby;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import play.mvc.WebSocket;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.java.SecureSocial;
import securesocial.core.java.SecuredAction;
import service.User;
import views.RenderService;

import com.google.inject.Inject;

public class Game extends Controller {
	private static final String DEFAULT_LOBBY = "default";
	public static Logger.ALogger logger = Logger.of("application.controllers.Game");
    private RuntimeEnvironment env;
    
    private static HashMap<String, Lobby> lobbys = new HashMap<>();

    /**
     * A constructor needed to get a hold of the environment instance.
     * This could be injected using a DI framework instead too.
     *
     * @param env
     */
    @Inject()
    public Game (RuntimeEnvironment env) {
        this.env = env;
    }

    @SecuredAction
    public Result play(String gameName) {
    	gameName = gameName.equals("") ? DEFAULT_LOBBY : gameName;
        if(logger.isDebugEnabled()){
            logger.debug("access granted to play");
        }
        User user = (User) ctx().args.get(SecureSocial.USER_KEY);
        if (!lobbys.containsKey(gameName)) {
        	lobbys.put(gameName, new Lobby());
        }
        Lobby lobby = lobbys.get(gameName);
        lobby.addPlayer(user);

        logger.info("Now " + lobby.getPlayerCount() + " Players in game " + gameName);
        if (logger.isDebugEnabled()) {
        	logger.debug("Players:");
        	for (User i : lobby.playerIter())
        		logger.debug(i.toString());
        }
        return ok(RenderService.renderGame(user, SecureSocial.env(), gameName));
    }

    @SecuredAction
    public synchronized WebSocket<String> getSocket() {
    	logger.debug("Get socket called");
        User user = (User) SecureSocial.currentUser(env).get(100);
        if (user == null)
        	return null;
        logger.debug("User has " + user.identities.size() + " identities.");
    	for (String gameName : lobbys.keySet()) {
    		logger.debug("Checking lobby " + gameName);
    		if (lobbys.get(gameName).containsPlayer(user))
    			return lobbys.get(gameName).getSocketForPlayer(user);
    	}
    	logger.error("User " + user.toString() + " requested WebSocket but is not part of any game.");
    	return WebSocket.reject(Results.badRequest("Player not in any game."));
    }
}
