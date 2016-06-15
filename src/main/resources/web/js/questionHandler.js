var questionHandler = (function() {
    var currentQuestion = null;
    var countdownInterval = null;
    var timeToAnswer = 0;
    var countdownDiv;
    var questionDiv;
    var answerButtons;

    var setCountdownDiv = function(div) {
        countdownDiv = div;
    };

    var setAnswerButtons = function(buttons) {
        answerButtons = buttons;
        answerButtons.forEach(function(b) {
            b.button.addEventListener("click", function() {
                questionHandler.guessAnswer(b.answer);
            });
        });
    };
    
    var setQuestionDiv = function(div) {
        questionDiv = div;
    };

    var newQuestion = function(question) {
        currentQuestion = question;
        startTimer();
        questionDiv.innerHTML = question.question;
        answerButtons[0].button.innerHTML = question.answerA;
        answerButtons[1].button.innerHTML = question.answerB;
        answerButtons[2].button.innerHTML = question.answerC;
        answerButtons[3].button.innerHTML = question.answerD;
        answerButtons[0].button.className = "btn btn-default";
        answerButtons[1].button.className = "btn btn-default";
        answerButtons[2].button.className = "btn btn-default";
        answerButtons[3].button.className = "btn btn-default";
    };

    var guessAnswer = function(answer) {
        answerButtons[currentQuestion.correctAnswer].button.className = "btn btn-success";
        if (answer !== currentQuestion.correctAnswer) {
            answerButtons[answer].button.className = "btn btn-danger";
        }
        messageHandler.sendMessage({
            type: "answer",
            username: username,
            questionId: currentQuestion.questionId,
            answer: answer
        });
    };

    var startTimer = function() {
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }
        timeToAnswer = 300;
        var countdown = function() {
            if (timeToAnswer > 0) {
                timeToAnswer--;
                countdownDiv.innerHTML = 'You still have ' + (parseInt(timeToAnswer / 10)) + '.' + (timeToAnswer % 10) +
                    ' second to answer';
            }
        };
        countdownInterval = setInterval(countdown, 100)
    };

    // register receiver for questions
    messageHandler.setReceiver("question", newQuestion);

    return {
        guessAnswer: guessAnswer,
        setCountdownDiv: setCountdownDiv,
        setAnswerButtons: setAnswerButtons,
        setQuestionDiv: setQuestionDiv
    }
})();