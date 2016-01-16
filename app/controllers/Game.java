package controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;

import play.Logger;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import securesocial.core.AuthenticationMethod;
import securesocial.core.BasicProfile;
import securesocial.core.RuntimeEnvironment;
import securesocial.core.java.SecureSocial;
import securesocial.core.java.SecuredAction;
import securesocial.core.java.UserAwareAction;
import service.DemoUser;
import views.html.game;

public class Game extends Controller {
	private static final String DEFAULT_LOBBY = "default";
	public static Logger.ALogger logger = Logger.of("application.controllers.Game");
    private RuntimeEnvironment env;
    
    private HashMap<String, List<DemoUser>> lobbys = new HashMap<>();
    
    private static String userToReadable(DemoUser u) {
    	return u.identities.get(0).fullName().get() + " (" + u.identities.get(0).userId() + ")";
    }

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
        DemoUser user = (DemoUser) ctx().args.get(SecureSocial.USER_KEY);
        if (!lobbys.containsKey(gameName)) {
        	lobbys.put(gameName, new LinkedList<DemoUser>());
        }
        List<DemoUser> players = lobbys.get(gameName);
        if (!players.contains(user))
        	players.add(user);
        logger.info("Now " + players.size() + " Players in game " + gameName);
        if (logger.isDebugEnabled()) {
        	logger.debug("Players:");
        	for (DemoUser i : players)
        		logger.debug(userToReadable(i));
        }
        return ok(game.render(user, SecureSocial.env(), gameName));
    }

    @SecuredAction
    public Result getBoard(String gameName) {
    	gameName = gameName.equals("") ? DEFAULT_LOBBY : gameName;
        DemoUser user = (DemoUser) ctx().args.get(SecureSocial.USER_KEY);
        
        return ok("1 1 2 2 3 3\n4 4 5 5 6 6");
    }

    @SecuredAction
    public Result getCard(String gameName, int x, int y) {
    	gameName = gameName.equals("") ? DEFAULT_LOBBY : gameName;
    	String[][] board = new String[][] {
    			new String[] {"1", "1", "2", "2", "3", "3"},
    			new String[] {"4", "4", "5", "5", "6", "6"}
    	};
        DemoUser user = (DemoUser) ctx().args.get(SecureSocial.USER_KEY);
        return ok(board[x][y]);
    }

    @SecuredAction
    public synchronized WebSocket<String> getSocket(DemoUser user) {
        return new WebSocket<String>(){
        	public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {};
        };
    }
}
