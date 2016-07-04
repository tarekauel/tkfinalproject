var app = angular
    .module("quizApp", ["ngRoute"])
    .factory("AuthService", [function () {
        var identity = {},
            login = function (username, rememberMe) {
                if (username.length < 3) {
                    return false;
                }

                // TODO: if remember is true, store username locally and retrieve it on loading
                identity.name = username;
                return true;
            },
            logout = function () { identity = {}; },
            isAuthenticated = function () { return "name" in identity; };

        return {
            getUsername: function () { return identity.name || ""; },
            isAuthenticated: isAuthenticated,
            login: login
        };
    }])
    .factory("LocationService", [function () {
        var getPosition = function(callback) {
            navigator.geolocation.getCurrentPosition(function(position) {
                callback(position.coords.latitude, position.coords.longitude);
            });
        };

        return {
            isAvailable: "geolocation" in navigator,
            getPosition: getPosition
        }
    }])
    .factory("MessageService", ['$rootScope', 'LocationService', function ($rootScope, LocationService) {
        var connection = new WebSocket('ws://' + location.hostname + ':' + (parseInt(location.port) + 1)),
            sendMessage = function(message) {
                // forward message to backend and append location information
                LocationService.getPosition(function(long, lat) {
                    message._pos = { longitude: long, latitude: lat };
                    connection.send(JSON.stringify(message));
                });
            },
            subscribe = function (scope, type, callback) {
                var handler = $rootScope.$on('message-rcv-' + type, function () {
                    callback.apply(scope, arguments);
                    scope.$apply();
                });
                scope.$on('$destroy', handler);
            };

        connection.onmessage = function (message) {
            var json = JSON.parse(message.data);
            console.log('Message: ', json);

            if (!json.type) {
                console.error("Received invalid message, unknown type " + json.type)
            } else {
                $rootScope.$emit('message-rcv-' + json.type, json);
            }
        };

        return {
            sendMessage: sendMessage,
            subscribe: subscribe
        };
    }])
    .config(["$locationProvider", "$routeProvider",
    function ($locationProvider, $routeProvider) {
        $locationProvider.hashPrefix("!");

        $routeProvider
            .when("/login", {
                controller: "LoginCtrl",
                templateUrl: "partials/login.html"
            })
            .when("/game", {
                controller: "GameCtrl",
                templateUrl: "partials/game.html"
            })
            .when("/questions", {
                controller: "QuestionCtrl",
                templateUrl: "partials/questions.html"
            })
            .otherwise("/login");

        console.log($locationProvider);
    }]);