package controllers;

import play.*;

import play.mvc.*;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.F;
import play.libs.Json;
//import play.libs.OpenID;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.WebSocket;

import de.htwg.memory.ui.TUI;

import com.fasterxml.jackson.databind.node.ObjectNode;
import views.html.*;
import java.nio.file.SecureDirectoryStream;
import java.util.*;
import java.io.File;

public class Application extends Controller {

	private static Map<String, de.htwg.memory.logic.Controller> controllers = new HashMap<>();
	private static Map<String, String> userDB = new HashMap<>();
	
	@play.mvc.Security.Authenticated(Secured.class)
    public Result index(){
		String email = session("email");
		de.htwg.memory.logic.Controller controller = controllers.get(email);
		return ok(views.html.index.render("HTWG Memory", controller, email));
		//"HTWG Memory", controller, email
	}
	
	
	public Result memory(String command) {
    	TUI tui = new TUI();
    	tui.processInputLine(command);
        return ok(tui.toString());
    }
    public Result login() {
        return ok( views.html.login.render(Form.form(User.class)));
    }
    
    public Result signupForm() {
        return ok(views.html.signup.render(Form.form(User.class)));
    }
    
    public Result authenticate() {
        Form<User> loginform = DynamicForm.form(User.class).bindFromRequest();

        User user = User.authenticate(loginform.get());

        if (loginform.hasErrors() || user == null) {
            ObjectNode response = Json.newObject();
            response.put("success", false);
            response.put("errors", loginform.errorsAsJson());
            if (user == null) {
                flash("errors", "Wrong username or password");
            }

            return badRequest(views.html.login.render(loginform));
        } else {
            session().clear();
            session("email", user.email);
            String email = session("email");
            de.htwg.memory.logic.Controller controller= de.htwg.memory.logic.Controller.getController();
            controllers.put(email,controller );
            return redirect(routes.Application.index());
        }
    }
    
    public Result signup() {
        Form<User> loginform = DynamicForm.form(User.class).bindFromRequest();

        ObjectNode response = Json.newObject();
        User account = loginform.get();
        boolean exists = userDB.containsKey(account.email);

        if (loginform.hasErrors() || exists) {
            response.put("success", false);
            response.put("errors", loginform.errorsAsJson());
            if (exists) {
                flash("errors", "Account already exists");
            }

            return badRequest(views.html.signup.render(loginform));
        } else {
            userDB.put(loginform.get().email, loginform.get().password);
            session().clear();
            session("email", loginform.get().email);
            return redirect(routes.Application.index());
        }
    }
    
	public static class Secured extends Security.Authenticator {
		
		@Override
		public String getUsername(Context context){
			return context.session().get("email");
		}
		@Override
		public Result onUnauthorized(Context context)
		{
			return redirect(routes.Application.login());
		}
		
	}
	public static class User {
        public String email;
        public String password;

        public User() { }

        private User(final String email, final String password) {
            this.email = email;
            this.password = password;
        }

     	public static User authenticate(User user){
     	    if (user != null && userDB.containsKey(user.email) && userDB.get(user.email).equals(user.password)) {
     	        return new User(user.email, user.password);
     	    }

    	    return null;
    	}
   }
	
	public Result auth() {
		/* TODO
        String providerUrl = "https://www.google.com/accounts/o8/id";
        String returnToUrl = "http://localhost:9000/openID/verify";

        Map<String, String> attributes = new HashMap<>();
        attributes.put("Email", "http://schema.openid.net/contact/email");
        attributes.put("FirstName", "http://schema.openid.net/namePerson/first");
        attributes.put("LastName", "http://schema.openid.net/namePerson/last");

        F.Promise<String> redirectUrl = OpenID.redirectURL(providerUrl, returnToUrl, attributes);
        return redirect(redirectUrl.get());
        */
        return redirect(
                routes.Application.index()
        );
    }

    public Result verify() {
    	/* TODO
        F.Promise<OpenID.UserInfo> userInfoPromise = OpenID.verifiedId();
        OpenID.UserInfo userInfo = userInfoPromise.get();
        session().clear();
        session("email", userInfo.attributes.get("Email"));
        */
        return redirect(
                routes.Application.index()
        );
    }

}


