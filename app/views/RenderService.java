package views;

import java.util.List;

import play.twirl.api.Content;
import securesocial.core.BasicProfile;
import securesocial.core.RuntimeEnvironment;
import service.User;
import views.html.game;
import views.html.index;
import views.html.linkResult;
import views.html.start;

public class RenderService {
	public static Content renderGame(User user, RuntimeEnvironment env, String gameName) {
		return game.render(user, env, gameName);
	}
	public static Content renderIndex(User user, RuntimeEnvironment env) {
		return index.render(user, env);
	}
	public static Content renderLinkResult(User user) {
		return linkResult.render(user, user.identities);
	}
	public static Content renderStart(User user, RuntimeEnvironment env) {
		return start.render(user, env);
	}
}
