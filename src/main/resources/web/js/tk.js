questionHandler.setCountdownDiv(document.getElementById('countdown'));

questionHandler.setAnswerButtons(['A', 'B', 'C', 'D'].map(function(c) {
    return {
      answer: c.charCodeAt(0) - 65,
      button: document.getElementById(c)
    };
  })
);

questionHandler.setQuestionDiv(document.getElementById("question"));


messageHandler.setReceiver("leader", function(message) {
  if (message.leader) {
    document.getElementById("leader").innerHTML = "Leader: Yes";
  } else {
    document.getElementById("leader").innerHTML =  "Leader: No";
  }
});

var login = function() {
    username = document.getElementById("username").value;
    messageHandler.sendMessage({
        type: "userinfo",
        username: username
    });
    document.getElementById("game").className = "";
    document.getElementById("login").className = "hidden";
};

document.getElementById("game").className = "hidden";

