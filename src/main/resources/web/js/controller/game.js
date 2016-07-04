angular.module("quizApp").controller("GameCtrl", ["$scope", "$interval", "AuthService", "MessageService",
function ($scope, $interval, AuthService, MessageService) {
    var selectedAnswer = null,
        countdownInterval = null,
        newQuestion = function(question) {
            selectedAnswer = null;
            $scope.question = question;
            startTimer();
        },
        startTimer = function() {
            stopTimer();

            $scope.remaining = 30;
            countdownInterval = $interval(function() {
                if ($scope.remaining > 0) {
                    $scope.remaining -= 0.1;
                } else {
                    $scope.remaining = 0;
                    stopTimer();
                }
            }, 100);
        },
        stopTimer = function () {
            if (angular.isDefined(countdownInterval)) {
                $interval.cancel(countdownInterval);
                countdownInterval = null;
            }
        }

    $scope.$on('$destroy', function () { stopTimer(); });

    $scope.isLeader = false;
    $scope.remaining = 0;
    $scope.question = {
        question: "Question is comming soon",
        answerA: "A",
        answerB: "B",
        answerC: "C",
        answerD: "D"
    };
    $scope.buttonClass = function (btnId) {
        return selectedAnswer === null || !("correctAnswer" in $scope.question) ? "btn-default" :
            selectedAnswer == $scope.question.correctAnswer && selectedAnswer == btnId  ? "btn-success" :
            "btn-danger";
    };
    $scope.guess = function (answer) {
        startTimer();
        selectedAnswer = answer;

        // Send answer option to server
        MessageService.sendMessage({
            type: "answer",
            username: AuthService.getUsername(),
            questionId: $scope.question.questionId,
            answer: answer
        });
    };

    MessageService.subscribe($scope, "score", function (type, message) {
        console.log("FROM GAMECTRL: ", type, message);
    });

    MessageService.subscribe($scope, "question", function (type, message) {
        newQuestion(message);
    });

    MessageService.subscribe($scope, "leader", function (type, message) {
        $scope.isLeader = message.leader;
    });

    console.log("GAME");
}]);