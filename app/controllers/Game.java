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
        if (logger.isInfoEnabled()) {
            logger.info("Now " + players.size() + " Players in game " + gameName);
        	logger.info("Players:");
        	for (DemoUser i : players)
        		logger.info(userToReadable(i));
        }
        return ok(game.render(user, SecureSocial.env(), gameName));
    }

    @SecuredAction
    public synchronized WebSocket<String> getSocket() {
    	final String userId = ((DemoUser) ctx().args.get(SecureSocial.USER_KEY)).identities.get(0).userId();
        return new WebSocket<String>(){
        	public void onReady(WebSocket.In<String> in, WebSocket.Out<String> out) {
                System.out.println("Init Socket for " + userId);

                in.onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        System.out.println(userId + " has quit the game");
                    }
                });
        	};
        };
    }
}
