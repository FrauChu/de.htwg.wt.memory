package model;

import java.util.HashMap;
import java.util.LinkedList;

import com.google.gson.Gson;

public class Response {
	private HashMap<String, Object> data;
	
	public Response() {
		data = new HashMap<>();
		clearActions();
	}
	
	public void setSize(int width, int height) {
		data.put("width", width);
		data.put("height", height);
	}
	
	public void setCards(String[][] cards) {
		data.put("cards", cards);
	}
	
	public void setActions(Iterable<String> actions) {
		LinkedList<Object> toPut = new LinkedList<>();
		for (Object o : actions)
			toPut.add(o);
		data.put("actions", toPut);
	}
	
	public void clearActions() {
		data.put("actions", new LinkedList<>());
	}
	
	public void setCurrentPlayer(String name) {
		data.put("currentPlayer", name);
	}
	
	@SuppressWarnings("unchecked")
	public void addAction(String action) {
		((LinkedList<Object>)data.get("actions")).add(action);
	}
	
	public String asJson() {
		return new Gson().toJson(data);
	}
}
