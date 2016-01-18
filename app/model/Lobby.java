package model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import play.Logger;
import play.libs.F;
import play.mvc.WebSocket;
import play.mvc.WebSocket.In;
import play.mvc.WebSocket.Out;
import service.User;
import de.htwg.memory.entities.Board;
import de.htwg.memory.logic.Controller;
import de.htwg.memory.logic.UiEventListener;

public class Lobby implements UiEventListener{
	public static Logger.ALogger logger = Logger.of("application.model.Lobby");
	private LinkedList<User> players = new LinkedList<>();
	private LinkedList<User> offlinePlayers = new LinkedList<>();
	private HashMap<String, In<String>> inputChannels = new HashMap<>();
	private HashMap<String, Out<String>> outputChannels = new HashMap<>();
	private LinkedList<String> chatHistory = new LinkedList<>();
	private final Controller gameController;
	private boolean needReload = false;
	
	public Lobby() {
		gameController = Controller.getNewController();
		gameController.loadBoard(new File("public/images/animals/game.dat"));
		gameController.addListener(this);
	}
	
	public boolean containsPlayer(User player) {
		boolean result = players.contains(player);
		if (!result) {
			for (User p : players)
				System.out.println("\t" + p.toString());
		}
		return result;
	}
	
	/**
	 * Adds player if not already in game.
	 * @param player Player to add
	 */
	public void addPlayer(final User player) {
		if (containsPlayer(player))
			return;
		logger.debug("Adding new player " + player);
		players.add(player);
        gameController.setPlayerCount(getPlayerCount());
	}
	
	public void removePlayer(User player) {
		if (!containsPlayer(player))
			return;
		players.remove(player);
		offlinePlayers.remove(player);
		inputChannels.remove(player.getId());
		outputChannels.remove(player.getId());
        gameController.setPlayerCount(getPlayerCount());
	}
	
	public Iterable<User> playerIter() {
		return players;
	}
	
	public int getPlayerCount() {
		return players.size();
	}
	
	public boolean isHisTurn(User player) {
		return players.get(gameController.getCurrentPlayer() - 1).equals(player);
	}
	
	private void playerLeft(final User player) {
        offlinePlayers.add(player);
        new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(60000);
					if (!offlinePlayers.contains(player))
						return;
					logger.info(player.toString() + " still offline. Removing from game.");
			        removePlayer(player);
				} catch (InterruptedException consumed) { }
			}
		}).start();
	}
	
	private void initWebSocket(final User player, final In<String> in, final Out<String> out) {
		inputChannels.put(player.getId(), in);
		outputChannels.put(player.getId(), out);
		
        in.onClose(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
            	logger.debug(player.toString() + " has left the game");
                playerLeft(player);
        		inputChannels.remove(player.getId());
        		outputChannels.remove(player.getId());
            }
        });
        in.onMessage(new F.Callback<String>() {
        	public void invoke(String a) throws Throwable {
        		try {
        			logger.debug("Got message " + a);
	        		Request req = new Gson().fromJson(a, Request.class);
	        		playerRequest(player, req);
        		} catch (Exception e) {
        			logger.error("Exception in message handling.", e);
        			throw e;
        		}
        	};
        });
	}
	
	public WebSocket<String> getSocketForPlayer(final User player) {
		if (offlinePlayers.contains(player)) {
			logger.debug(player.toString() + " rejoined in time.");
			offlinePlayers.remove(player);
		}
		return new WebSocket<String>() {
			@Override
			public void onReady(final In<String> in, final Out<String> out) {
				initWebSocket(player, in, out);
			}
		};
	}
	
	private void playerRequest(User player, Request req) {
		if (req.action.equals("keep-alive")) {
			logger.trace("Got keep-alive. Doing nothing.");
		} else if (req.action.equals("chat")) {
			addChatMessage(player, req.message);
		} else if (req.action.equals("get")) {
			outputChannels.get(player.getId()).write(fullStatus().asJson());
		} else if (req.action.equals("restart")) {
			logger.trace("Calling restart");
			gameController.resetGame();
		} else if (needReload && isHisTurn(player)) {
			logger.trace("Calling hide wrong");
			gameController.hideWrongMatch();
			needReload = false;
		} else if (req.action.equals("pick") && isHisTurn(player)) {
			logger.trace("Picking card");
			if (!gameController.pickCard(req.x, req.y)) {
				logger.debug("Choice was invalid.");
			}
		}
	}
	
	private void addChatMessage(User user, String message) {
		chatHistory.add(user.getName() + ": " + message);
		Response resp = new Response();
		resp.setChatHistory(chatHistory);
		sendToAll(resp.asJson());
	}
	
	private void sendToAll(String message) {
		for (Out<String> channel : outputChannels.values()) {
			channel.write(message);
		}
	}
	
	private Response getBoardResp() {
		Response resp = new Response();
		resp.setCards(boardToField(gameController.getBoard()));
		return resp;
	}
	
	private Response fullStatus() {
		Response resp = new Response();
		Board b = gameController.getBoard();
		resp.setSize(b.getWidth(), b.getHeight());
		resp.setCards(boardToField(b));
		resp.setRound(gameController.getRoundNumber());
		resp.setChatHistory(chatHistory);
		if (players.size() > 0) {
			resp.setCurrentPlayer(players.get(gameController.getCurrentPlayer() - 1).getName());
			resp.setPlayerList(playerNames());
		}
		
		return resp;
	}
	
	private String[] playerNames() {
		String[] result = new String[players.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = players.get(i).getName();
		return result;
	}
	
	private static String[][] boardToField(Board b) {
		String[][] cards = new String[b.getHeight()][b.getWidth()];
		for (int i = 0; i < b.getHeight(); i++) {
			for (int j = 0; j < b.getWidth(); j++) {
				cards[i][j] = b.getCard(i, j).toString().trim();
			}
		}
		return cards;
	}

	@Override
	public void boardChanged() {
		Response resp = getBoardResp();
		sendToAll(resp.asJson());
	}

	@Override
	public void boardNeedsRealod() {
		Response resp = getBoardResp();
		sendToAll(resp.asJson());
	}

	@Override
	public void gameReset() {
		sendToAll(fullStatus().asJson());
	}

	@Override
	public void matchMade() {
		Response resp = getBoardResp();
		sendToAll(resp.asJson());
	}

	@Override
	public void noMatchMade() {
		needReload = true;
		Response resp = getBoardResp();
		resp.setCurrentPlayer(players.get(gameController.getCurrentPlayer() - 1).getName());
		resp.setRound(gameController.getRoundNumber());
		sendToAll(resp.asJson());
	}

	@Override
	public void playerCountChanged(int playerCount) {
		sendToAll(fullStatus().asJson());
	}

	@Override
	public void win() {
		Response resp = fullStatus();
		JsonObject action = new JsonObject();
		action.addProperty("action", "end");
		action.addProperty("rounds", gameController.getRoundNumber());
		action.addProperty("winner", players.get(gameController.getCurrentPlayer() - 1).getName());
		JsonArray playerScores = new JsonArray();
		for (int i = 0; i < gameController.getPlayerCount(); i++) {
			JsonObject playerScore = new JsonObject();
			playerScore.addProperty("name", players.get(i).getName());
			playerScore.addProperty("points", gameController.getPlayerMatches(i));
			playerScores.add(playerScore);
		}
		action.add("playerScores", playerScores);
		resp.addAction(action);
		sendToAll(resp.asJson());
	}
}
