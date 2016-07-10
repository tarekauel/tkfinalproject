angular.module("quizApp").controller("LoginCtrl", ["$scope", "$location", "AuthService", "MessageService",
 function ($scope, $location, AuthService, MessageService) {
    $scope.login = function () {
        if (AuthService.login($scope.username, $scope.rememberMe)) {
            MessageService.sendMessage({
                type: "userinfo",
                username: $scope.username
            });
            $location.path("/game");
        }
    };
}]);