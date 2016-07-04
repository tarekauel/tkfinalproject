angular.module('quizApp').controller('QuestionCtrl', ['$scope', 'QuestionService', function ($scope, QuestionService) {
    QuestionService.populate();

    $scope.questions = QuestionService.load();

    // TODO: Define filters to display coordinates and map correct answer index (0-3) to characters (A-Z)
}]);