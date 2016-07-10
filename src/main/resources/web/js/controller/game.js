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
        question: "Awaiting question...",
        answerA: "A",
        answerB: "B",
        answerC: "C",
        answerD: "D"
    };
    $scope.scores = {};
    $scope.isScoresEmpty = function () {
        return Object.keys($scope.scores).length == 0;
    };
    $scope.buttonClass = function (btnId) {
        return selectedAnswer === null || selectedAnswer != btnId || !("correctAnswer" in $scope.question) ? "btn-default" :
            selectedAnswer == $scope.question.correctAnswer  ? "btn-success" :
            "btn-danger";
    };
    $scope.guess = function (answer) {
        if (selectedAnswer == null) {
            selectedAnswer = answer;

            MessageService.sendMessage({
                type: "answer",
                username: AuthService.getUsername(),
                questionId: $scope.question.questionId,
                answer: answer
            });
        }
    };

    MessageService.subscribe($scope, "score", function (type, message) {
        $scope.scores = [];

        Object.keys(message.scores).forEach(function (key) {
            $scope.scores.push({ username: key, score: message.scores[key] });
        });

        console.log($scope.scores);
    });

    MessageService.subscribe($scope, "question", function (type, message) {
        newQuestion(message);
    });

    MessageService.subscribe($scope, "leader", function (type, message) {
        $scope.isLeader = message.leader;
    });
}]);