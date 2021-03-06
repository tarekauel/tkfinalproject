package umundo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import helper.Database;
import org.apache.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import umundo.control.Client;
import umundo.messages.out.QuestionList;
import umundo.model.*;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class WSServer extends WebSocketServer {

  private static Logger log = Logger.getLogger(WSServer.class.getName());

  private Client client;
  private WebSocket webSocket;
  private static final Gson gson = new Gson();
  private boolean loggedIn = false;

  public WSServer(Client client, int port) throws UnknownHostException {
    super(new InetSocketAddress(port));
    this.client = client;
    log.info(String.format("Server created on port %d", port));
  }

  public void sendMessage(OutMessage m) {
    sendMessage(m, true);
  }

  public void sendMessage(OutMessage m, boolean loginRequired) {
    if (webSocket != null && (!loginRequired || loggedIn)) {
      webSocket.send(gson.toJson(m));
      log.info(String.format("Message was sent: %s", gson.toJson(m)));
    }
  }

  public void setLoggedIn(boolean loggedIn) {
    this.loggedIn = loggedIn;
  }

  @Override
  public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
    log.info("Someone connected");
    this.webSocket = webSocket;
    client.pullScoreboard();
  }

  @Override
  public void onClose(WebSocket webSocket, int i, String s, boolean b) {
    if (this.webSocket == webSocket) {
      // user closed the browser tab
      this.webSocket = null;
    }
  }

  @Override
  public void onMessage(WebSocket webSocket, String s) {
    log.info("received message from ui: " + s);
    JsonObject jo = new JsonParser().parse(s).getAsJsonObject();

    try {
      String type = jo.get("type").getAsString();
      jo.remove("type");
      if (type.equals("answer")) {
        Answer a = gson.fromJson(jo, Answer.class);
        client.setLatestPos(a.getPos());
        client.answerFromUser(a);
      } else if (type.equals("userinfo")) {
        Userinfo info = gson.fromJson(jo, Userinfo.class);
        client.setLatestPos(info.getPos());
        client.setUser(info);
      } else if (type.equals("question-db")) {
        String[] exclude = gson.fromJson(jo.getAsJsonArray("exclude"), String[].class);
        sendMessage(new QuestionList(Question.loadAllById(exclude, true)), false);
      } else if (type.equals("question-update")) {
        Question q = gson.fromJson(jo.getAsJsonObject("question"), Question.class);
        boolean isNewRecord = q.getQuestionId() == null;
        q.save();

        if (isNewRecord) {
          sendMessage(new QuestionList(q), false);
        }
      } else if (type.equals("send-scoreboard")) {
        sendMessage(Database.getGlobalScores(), false);
        log.info("Sent global scoreboard");
      }
    } catch (Exception e) {
      log.error("Failed to deserialize JSON: " + e.toString());
      e.printStackTrace();
    }
  }

  @Override
  public void onError(WebSocket webSocket, Exception e) {

  }
}
