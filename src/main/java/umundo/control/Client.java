package umundo.control;

import helper.Database;
import org.apache.log4j.Logger;
import umundo.QuestionFactory;
import umundo.SimpleWebServer;
import umundo.WSServer;
import org.umundo.core.Discovery;
import org.umundo.core.Discovery.DiscoveryType;
import org.umundo.core.Message;
import org.umundo.core.Node;
import org.umundo.core.SubscriberStub;
import umundo.model.*;
import org.umundo.s11n.ITypedGreeter;
import org.umundo.s11n.ITypedReceiver;
import org.umundo.s11n.TypedPublisher;
import org.umundo.s11n.TypedSubscriber;
import umundo.model.Question;
import umundo.model.InMessage.Pos;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class Client {

  // log4j logger
  private static Logger log = Logger.getLogger(Client.class.getName());

  private Node gameNode;

  private long lastHeartbeat;
  private long priority = System.currentTimeMillis();

  private TypedSubscriber subscriber;
  private TypedPublisher publisher;

  // indicates if this node is the leader right now
  private boolean leader = false;

  // websocket server to communicate with the web ui
  private WSServer wsServer;

  private String currentQuestionId = "";
  private ArrayList<Question> questionHistory = new ArrayList<>();
  // map of scores: (username, score)
  private HashMap<String, Integer> scoreboard = new HashMap<>();
  // map of uuid: (username, uuid)
  private HashMap<String, String> uuidmap = new HashMap<>();
  private HashSet<String> receivedAnswer;

  private String username;

  private boolean electionGoesOn = true;
  private Discovery disc;

  private final Client self = this;
  private Thread workerThread = Thread.currentThread();

  private umundo.model.InMessage.Pos latestPos = null;

  public Client(int port) {
    log.info(String.format("Starting new client on port %d and %d",
        port, port + 1));
    this.startUiServer(port);
  }

  private void startUmundo() {
    log.info("Starting umundo");
    disc = new Discovery(DiscoveryType.MDNS);
    gameNode = new Node();
    disc.add(gameNode);

    // channel for sending and receiving questions
    String QUESTION_CHANNEL = "GAME_CHANNEL";
    subscriber = new TypedSubscriber(QUESTION_CHANNEL, new Receiver(this));
    publisher = new TypedPublisher(QUESTION_CHANNEL);
    publisher.setGreeter(new Greeter());
    gameNode.addPublisher(publisher);
    gameNode.addSubscriber(subscriber);

    this.checkSubscription();
    this.heartbeatSender();
  }

  private void checkSubscription() {
    (new Thread() {
      @Override
      public void run() {
        while(true) {
          try {
            Thread.sleep(1000);
            int count = publisher.waitForSubscribers(0);
            if (count == 0) {
              // due to some uMundo bug this is needed. This avoids that nodes loose the
              // connection to each other
              disc.remove(gameNode);
              Thread.sleep(100);
              disc.add(gameNode);
            }
            log.info("Number of subscribers: " + count);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  public Pos getLatestPos() {
    return latestPos;
  }

  public void setLatestPos(umundo.model.InMessage.Pos latestPos) {
    this.latestPos = latestPos;
  }

  private void heartbeatSender() {
    // the leader sends a heartbeat to indicate that he is up.
    // non-leader node check if they have received a heartbeat within the last
    // five seconds, if not, the leader seems to be down and a leader election
    // has to start
    log.info("Heartbeat sender started");
    self.lastHeartbeat = System.currentTimeMillis();
    (new Thread() {
      @Override
      public void run() {
        boolean run = true;
        while(run) {
          try {
            Thread.sleep(2000);
            if (leader) {
              log.info("Leader sends heartbeat");
              publisher.send(new Heartbeat().get());
            } else {
              if (self.lastHeartbeat < System.currentTimeMillis() - 1000 * 5) {
                log.info("Detected no heartbeat for three seconds, start election.");
                // last heartbeat older than 5 secs, leader is considered to be down
                run = false;
                self.leaderElection();
              }
            }
            // send update info to client
            self.wsServer.sendMessage(new LeaderInfo(leader));
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  private void leaderElection() {
    // each client has a priority (starting time as long value). A smaller value
    // indicates a higher priority. If a message is received from a node with a
    // higher priority, this node can be sure, that the other one will become the
    // leader. If a node does not receive a message from a node with a higher priority
    // within 5 seconds, the node will become the new leader.
    electionGoesOn = true;
    (new Thread() {
      @Override
      public void run() {
        while (electionGoesOn) {
          try {
            Thread.sleep(250);
            publisher.send(new Priority(self.priority).get());
            log.info("Send my prio");

          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();

    (new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(5000);
          // if election still goes on --> this is the new leader
          self.leader = self.electionGoesOn;
          self.electionGoesOn = false;
          log.info("election finished and am I the leader: " + self.leader);
          self.heartbeatSender();
          if (currentQuestionId.isEmpty()) {
            // trigger first question immediately
            workerThread.interrupt();
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  public String getUsername() {
    return username;
  }

  private void startUiServer(int port) {
    // Start a small web server that serves the ui
    (new Thread(){
      @Override
      public void run() {
        try {
          SimpleWebServer.start(port);
        } catch (Exception e) {
          System.err.println("Error while starting the ui server");
          e.printStackTrace();
          System.exit(1);
        }
      }
    }).start();

    // start the web socket server that is used to get the user inputs
    try {
      wsServer = new WSServer(this, port + 1);
      wsServer.start();
    } catch (UnknownHostException e) {
      System.err.println("Error while starting the web socket server");
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void run() {
    while(true) {
      try {
        if (this.leader) {
          // if leader: send question
          // publish a new question if client is the leader
          Question q = QuestionFactory.getQuestionForLocation(this.getLatestPos());
          q.setMatchUUID(UUID.randomUUID().toString());
          currentQuestionId = q.getQuestionId();
          questionHistory.add(q);
          publisher.send(q.get());
          receivedAnswer = new HashSet<>();
          receivedQuestion(q);
          log.info("leader sent message");
        }
        log.info("question thread running");
        Thread.sleep(30000); // 30 seconds between two questions
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void exit() {
    // method to kindly shutdown the node
    log.info("Going to quit");
    System.exit(0);
  }

  public void setUser(Userinfo user) {
    this.username = user.getUsername();
    this.scoreboard.put(username, 0);
    this.uuidmap.put(username, Database.getMyUID());
    wsServer.setLoggedIn(true);
    this.startUmundo();
  }

  private void receivedQuestion(Question q) {
    // keep track of the question id in order to be able to take over as new leader without
    // confusing ids
    currentQuestionId = q.getQuestionId();
    questionHistory.add(q);
    log.info("Received question with id " + q.getQuestionId());
    wsServer.sendMessage(q);
  }

  public void pullScoreboard() {
    // ui pulls the latest scoreboard if someone connects to the server
    wsServer.sendMessage(new Scoreboard(this.scoreboard, this.uuidmap));
  }

  public void answerFromUser(Answer answer) {
    log.info("Received answer from UI, question " + answer.getQuestionId());

    if (this.leader) {
      receivedAnswer(answer);
    } else {
      log.info("Send answer to leader");
      this.publisher.send(answer.get());
    }
  }

  private void sendWelcome() {
    log.info("Sending welcome message");
    Welcome w = new Welcome(this.getUsername(), Database.getMyUID(), SyncManager.getInstance().getHashes());
    this.receivedWelcome(w);
    publisher.send(w.get());
  }

  private void receivedPriority(Priority p) {
    if (p.getPriority() < this.priority) {
      log.info("Received a message from someone with a higher priority");
      // someone is active who will become the master before this node will be the leader
      electionGoesOn = false;
    }
  }

  private void receivedAnswer(Answer a) {
    if (this.leader) {
      log.info(String.format("Received answer by %s for %s", a.getUsername(), a.getQuestionId()));

      if (a.getQuestionId().equals(currentQuestionId) && !receivedAnswer.contains(a.getUsername())) {
        // is latest question and user has not answered yet
        receivedAnswer.add(a.getUsername());
        Question q = questionHistory.get(questionHistory.size() - 1);
        if (a.getAnswer() == q.getCorrectAnswer()) {
          if (!q.isAnsweredCorrectly()) {
            log.info(String.format("Answer by %s for %s was correct", a.getUsername(), a.getQuestionId()));
            // Remember that it was answered correctly
            q.setAnsweredCorrectly();

            //update scores
            int lastScore = scoreboard.getOrDefault(a.getUsername(), 0);
            scoreboard.put(a.getUsername(), lastScore + 1);

            // Add to database
            Database.insertMatch(new Match(q.getMatchUUID(), new Player(a.getUsername(), uuidmap.get(a.getUsername()))));

            Scoreboard sb = new Scoreboard(scoreboard, uuidmap);
            publisher.send(sb.get());
            receivedScoreboard(sb);
          } else {
            log.info(String.format("Answer by %s for %s was correct, but question was already answered", a.getUsername(), a.getQuestionId()));
          }
        } else {
          log.info(String.format("Answer by %s for %s was wrong", a.getUsername(), a.getQuestionId()));
        }
      } else {
        // question is outdated
        log.info(String.format("Answer by %s was too late. our qid: %s, their qid: %s", a.getUsername(), currentQuestionId, a.getQuestionId()));
      }
    }
  }

  private void receivedHeartbeat(Heartbeat h) {
    // if a node received a heartbeat it is not the leader.
    // corner case: two nodes think they are the leader. they will both deactivate each other
    // and elect a new leader following the protocol.
    log.info("received heartbeat");
    this.lastHeartbeat = h.getTimestamp();
    this.leader = false;
    this.electionGoesOn = false;
  }

  private void receivedScoreboard(Scoreboard scoreboard) {
    HashMap<String, Integer> newScore = scoreboard.getScores();
    HashMap<String, String> newUUIDs = scoreboard.getUUIDmap();
    for (String user : newScore.keySet()) {
      int oldscore = this.scoreboard.getOrDefault(user, -1);
      // If no old score is known, ignore this user
      if (oldscore == -1) continue;
      // If score has increased, write to database as winner
      if (oldscore < newScore.get(user)) {
        Database.insertMatch(new Match(
                questionHistory.get(questionHistory.size() - 1).getMatchUUID(),
                new Player(user, newUUIDs.get(user))
        ));
        break;
      }
    }

    this.scoreboard = scoreboard.getScores();
    log.info("Received latest scoreboard");
    wsServer.sendMessage(scoreboard);
  }

  private void receivedWelcome(Welcome w) {
    this.scoreboard.put(w.getUsername(), this.scoreboard.getOrDefault(w.getUsername(), 0));
    this.uuidmap.put(w.getUsername(), w.getUUID());
    this.receivedScoreboard(new Scoreboard(this.scoreboard, this.uuidmap));

    // Process sync information in welcome message
    log.info("Processing Welcome sync information");
    Message welcomeReply = SyncManager.getInstance().handleSyncMessage(w);
    if (welcomeReply != null) publisher.send(welcomeReply);
  }

  private void receivedScoreSync(ScoreSyncMessage msg) {
    Message reply = SyncManager.getInstance().handleSyncMessage(msg);
    if (reply != null) publisher.send(reply);
  }

  private static class Receiver implements ITypedReceiver {

    private Client client;

    private Receiver(Client client) {
      this.client = client;
    }

    public void receiveObject(Object o, Message message) {
      // type determines the message type
      String type = message.getMeta("type");
      log.info("Received message of type " + type);
      switch (type) {
        case "question": client.receivedQuestion(Question.fromMessage(message)); break;
        case "answer": client.receivedAnswer(Answer.fromMessage(message)); break;
        case "score": client.receivedScoreboard(Scoreboard.fromMessage(message)); break;
        case "heartbeat": client.receivedHeartbeat(Heartbeat.fromMessage(message)); break;
        case "priority": client.receivedPriority(Priority.fromMessage(message)); break;
        case "welcome": client.receivedWelcome(Welcome.fromMessage(message)); break;
        case "sync": client.receivedScoreSync(ScoreSyncMessage.fromMessage(message)); break;
        default: log.error("Received unknown message type");
      }
    }
  }

  private class Greeter implements ITypedGreeter {
    @Override
    public void welcome(TypedPublisher typedPublisher, SubscriberStub subscriberStub) {
      log.info("=> " + typedPublisher.toString());
      log.info("=> " + subscriberStub.toString());
      self.sendWelcome();
    }

    @Override
    public void farewell(TypedPublisher typedPublisher, SubscriberStub subscriberStub) {
      log.info("<= " + typedPublisher.toString());
      log.info("<= " + subscriberStub.toString());
    }
  }
}