package model;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;

import com.google.gson.Gson;

import play.libs.F;
import play.mvc.WebSocket;
import play.mvc.WebSocket.In;
import play.mvc.WebSocket.Out;
import service.DemoUser;
import de.htwg.memory.entities.Board;
import de.htwg.memory.logic.Controller;
import de.htwg.memory.logic.UiEventListener;

public class Lobby implements UiEventListener{
	private LinkedList<DemoUser> players = new LinkedList<>();
	private LinkedList<DemoUser> offlinePlayers = new LinkedList<>();
	private HashMap<String, In<String>> inputChannels = new HashMap<>();
	private HashMap<String, Out<String>> outputChannels = new HashMap<>();
	private final Controller gameController;
	private boolean needReload = false;
	
	public Lobby() {
		gameController = Controller.getController();
		gameController.loadBoard(new File("public/images/animals/game.dat"));
		gameController.addListener(this);
	}
	
	public boolean containsPlayer(DemoUser player) {
		boolean result = players.contains(player);
		if (!result) {
			for (DemoUser p : players)
				System.out.println("\t" + p.getHumanReadable());
		}
		return result;
	}
	
	/**
	 * Adds player if not already in game.
	 * @param player Player to add
	 */
	public void addPlayer(final DemoUser player) {
		if (containsPlayer(player))
			return;
		System.out.println("Adding new player " + player);
		players.add(player);
        gameController.setPlayerCount(getPlayerCount());
	}
	
	public void removePlayer(DemoUser player) {
		if (!containsPlayer(player))
			return;
		players.remove(player);
		if (offlinePlayers.contains(player))
			offlinePlayers.remove(player);
		inputChannels.remove(player.getId());
		outputChannels.remove(player.getId());
        gameController.setPlayerCount(getPlayerCount());
	}
	
	public Iterable<DemoUser> playerIter() {
		return players;
	}
	
	public int getPlayerCount() {
		return players.size();
	}
	
	public boolean isHisTurn(DemoUser player) {
		return players.get(gameController.getCurrentPlayer() - 1).equals(player);
	}
	
	private void playerLeft(final DemoUser player) {
        offlinePlayers.add(player);
        new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(60000);
					if (!offlinePlayers.contains(player))
						return;
					System.out.println(player.getHumanReadable() + " still offline. Removing from game.");
			        removePlayer(player);
				} catch (InterruptedException consumed) { }
			}
		}).start();
	}
	
	private void initWebSocket(final DemoUser player, final In<String> in, final Out<String> out) {
		inputChannels.put(player.getId(), in);
		outputChannels.put(player.getId(), out);
		
        in.onClose(new F.Callback0() {
            @Override
            public void invoke() throws Throwable {
                System.out.println(player.getHumanReadable() + " has left the game");
                playerLeft(player);
            }
        });
        in.onMessage(new F.Callback<String>() {
        	public void invoke(String a) throws Throwable {
        		try {
	        		System.out.println("Got message " + a);
	        		Request req = new Gson().fromJson(a, Request.class);
	        		playerRequest(player, req);
        		} catch (Exception e) {
        			System.err.println("Exception in message handling.");
        			e.printStackTrace();
        			throw e;
        		}
        	};
        });
	}
	
	public WebSocket<String> getSocketForPlayer(final DemoUser player) {
		if (offlinePlayers.contains(player)) {
			System.out.println(player.getHumanReadable() + " rejoined in time.");
			offlinePlayers.remove(player);
		}
		return new WebSocket<String>() {
			@Override
			public void onReady(final In<String> in, final Out<String> out) {
				initWebSocket(player, in, out);
			}
		};
	}
	
	private void playerRequest(DemoUser player, Request req) {
		if (req.action.equals("keep-alive")) {
			System.out.println("Got keep-alive. Doing nothing.");
		} else if (req.action.equals("get")) {
			outputChannels.get(player.getId()).write(fullStatus().asJson());
		} else if (req.action.equals("restart")) {
			System.out.println("Calling restart");
			gameController.resetGame();
		} else if (needReload && isHisTurn(player)) {
			System.out.println("Calling hide wrong");
			gameController.hideWrongMatch();
			needReload = false;
		} else if (req.action.equals("pick")) {
			System.out.println("Picking card");
			if (!gameController.pickCard(req.x, req.y)) {
				System.out.println("Choice was invalid.");
			}
		}
	}
	
	private void sendToAll(String message) {
		for (Out<String> channel : outputChannels.values()) {
			channel.write(message);
		}
	}
	
	private void sendBoardUpdate() {
		Response resp = new Response();
		resp.setCards(boardToField(gameController.getBoard()));
		sendToAll(resp.asJson());
	}
	
	private Response fullStatus() {
		Response resp = new Response();
		Board b = gameController.getBoard();
		resp.setSize(b.getWidth(), b.getHeight());
		resp.setCards(boardToField(b));
		return resp;
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
		sendBoardUpdate();
	}

	@Override
	public void boardNeedsRealod() {
		sendBoardUpdate();
	}

	@Override
	public void gameReset() {
		sendToAll(fullStatus().asJson());
	}

	@Override
	public void matchMade() {
		sendBoardUpdate();
	}

	@Override
	public void noMatchMade() {
		needReload = true;
		sendBoardUpdate();
	}

	@Override
	public void playerCountChanged(int playerCount) {
		sendToAll(fullStatus().asJson());
	}

	@Override
	public void win() {
		Response resp = fullStatus();
		resp.addAction("end");
		sendToAll(resp.asJson());
	}
}
