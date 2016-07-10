angular.module('quizApp')
.controller('ScoresCtrl', ['$scope', '$location', 'MessageService', function ($scope, $location, MessageService) {
    MessageService.subscribe($scope, "global-scoreboard", function(type, message) {
        console.log("Received log, binding to scope");
        $scope.scores = message.scores;
    });
    console.log("Requesting global scoreboard");
    MessageService.sendMessage({ 'type': 'send-scoreboard' });

}]);