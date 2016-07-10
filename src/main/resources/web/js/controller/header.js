angular.module('quizApp').controller('HeaderCtrl', ['$scope', '$location', 'AuthService', function ($scope, $location, AuthService) {
    $scope.isActive = function (path) {
        return $location.path() == path;
    };

    $scope.isAuthenticated = AuthService.isAuthenticated.bind(AuthService);
}]);