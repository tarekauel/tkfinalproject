angular.module('quizApp')
.controller('QuestionCtrl', ['$scope', '$location', 'QuestionService', function ($scope, $location, QuestionService) {
    $scope.questions = QuestionService.getQuestions();
    $scope.create = function () {
        $location.path('/questions/create');
    };
    $scope.edit = function (questionId) {
        $location.path('/questions/' + questionId);
    };

    QuestionService.onUpdate($scope, function () {
        $scope.questions = QuestionService.getQuestions();
    });
}])
.controller('QuestionEditCtrl', ['$scope', '$routeParams', '$location', 'LocationService', 'QuestionService',
function ($scope, $routeParams, $location, LocationService, QuestionService) {
    $scope.question = QuestionService.get($routeParams.questionId);
    $scope.linkToLocation = !_.isEmpty($scope.question.pos);
    $scope.pos = $scope.question.pos || {};

    $scope.updateCoordinates = function () {
        LocationService.getPosition(function(long, lat) {
            $scope.pos.longitude = long;
            $scope.pos.latitude = lat;
            $scope.linkToLocation = true;
            $scope.$apply();
        });
    }

    $scope.save = function () {
        if ($scope.linkToLocation) {
            $scope.question.pos = $scope.pos;
        } else {
            delete $scope.question.pos;
        }

        QuestionService.addOrUpdate($scope.question);
        $location.path('/questions');
    };
}])
.controller('QuestionCreateCtrl', ['$scope', '$location', 'LocationService', 'QuestionService',
function ($scope, $location, LocationService, QuestionService) {
    $scope.question = {};
    $scope.linkToLocation = false;
    $scope.pos = {};

    $scope.updateCoordinates = function () {
        LocationService.getPosition(function(long, lat) {
            $scope.pos.longitude = long;
            $scope.pos.latitude = lat;
            $scope.linkToLocation = true;
            $scope.$apply();
        });
    }

    $scope.save = function () {
        if ($scope.linkToLocation) {
            $scope.question.pos = $scope.pos;
        }

        QuestionService.addOrUpdate($scope.question);
        $location.path('/questions');
    };

    $scope.updateCoordinates();
}]);