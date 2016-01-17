package model;

import java.util.HashMap;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

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
	
	public void setActions(Iterable<JsonElement> actions) {
		LinkedList<Object> toPut = new LinkedList<>();
		for (Object o : actions)
			toPut.add(o);
		data.put("actions", toPut);
	}
	
	public void clearActions() {
		data.put("actions", new LinkedList<>());
	}
	
	@SuppressWarnings("unchecked")
	public void addAction(JsonElement action) {
		((LinkedList<Object>)data.get("actions")).add(action);
	}
	
	public void setCurrentPlayer(String name) {
		data.put("currentPlayer", name);
	}
	
	public void setPlayerList(String[] names) {
		data.put("players", names);
	}
	
	public void setPlayerScores(String[] scores) {
		data.put("scores", scores);
	}
	
	public void setRound(int round) {
		data.put("round", round);
	}
	
	public String asJson() {
		return new Gson().toJson(data);
	}
}
