package models;
import de.htwg.memory.logic.UiEventListener;
import play.mvc.WebSocket;
import play.mvc.WebSocket.Out;
import de.htwg.memory.logic.Controller;


public class Listener implements UiEventListener{

	private Out<String> out;
	private de.htwg.memory.logic.Controller controller;


	public Listener(de.htwg.memory.logic.Controller controller,WebSocket.Out<String> out) {
		controller.addListener(this);
		this.controller = controller;
		this.out = out;
	}

	@Override
	public void boardChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void boardNeedsRealod() {
		System.out.println("WUI was updated");
		
	}

	@Override
	public void gameReset() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void matchMade() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void noMatchMade() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void playerCountChanged(int arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void win() {
		// TODO Auto-generated method stub
		
	}
	

}
