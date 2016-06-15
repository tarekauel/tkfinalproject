questionHandler.setCountdownDiv(document.getElementById('countdown'));

questionHandler.setAnswerButtons(['A', 'B', 'C', 'D'].map(function(c) {
    return {
      answer: c.charCodeAt(0) - 65,
      button: document.getElementById(c)
    };
  })
);


messageHandler.setReceiver("leader", function(message) {
  if (message.leader) {
    document.getElementById("leader").innerHTML = "Leader: Yes";
  } else {
    document.getElementById("leader").innerHTML =  "Leader: No";
  }
});
