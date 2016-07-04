var app = angular
    .module("quizApp", ["ngRoute", "LocalStorageModule"])
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
    .factory("MessageService", ["$rootScope", "LocationService", function ($rootScope, LocationService) {
        var connection = new WebSocket("ws://" + location.hostname + ":" + (parseInt(location.port) + 1)),
            sendMessage = function(message) {
                // forward message to backend and append location information
                LocationService.getPosition(function(long, lat) {
                    message._pos = { longitude: long, latitude: lat };
                    connection.send(JSON.stringify(message));
                });
            },
            subscribe = function (scope, type, callback) {
                var handler = $rootScope.$on("message-rcv-" + type, function () {
                    callback.apply(scope, arguments);
                    scope.$apply();
                });
                scope.$on("$destroy", handler);
            };

        connection.onmessage = function (message) {
            var json = JSON.parse(message.data);
            console.log("Message: ", json);

            if (!json.type) {
                console.error("Received invalid message, unknown type " + json.type)
            } else {
                $rootScope.$emit("message-rcv-" + json.type, json);
            }
        };

        return {
            sendMessage: sendMessage,
            subscribe: subscribe
        };
    }])
    .factory("QuestionService", ["$q", "$rootScope", "localStorageService", "MessageService",
    function ($q, $rootScope, localStorageService, MessageService) {
        var questions = {},
            addOrUpdate = function (question) {
                var q = _(question).pick("questionId", "question", "answerA", "answerB", "answerC", "answerD", "correctAnswer", "pos"),
                    json = JSON.stringify(q);

                if (localStorageService.get(q.questionId) !== json) {
                    localStorageService.set(q.questionId, json);
                    questions[q.questionId] = q;
                }
            },
            populate = function (excludeKnownIds) {
                excludeKnownIds = excludeKnownIds !== false;
                message = { type: "question-db", exclude: [] };
                if (excludeKnownIds) message.exclude = localStorageService.keys();
                MessageService.sendMessage(message);
            },
            reset = function () {
                localStorageService.clearAll();
                questions = {};
            },
            sync = function () { };

        // Load questions from local storage
        _(localStorageService.keys()).each(function (questionId) {
            questions[questionId] = JSON.parse(localStorageService.get(questionId));
        });

        // Subscribe to new questions and insert/update them into the local storage on demand
        MessageService.subscribe($rootScope, "question", function (type, msg) {
            addOrUpdate(msg);
        });

        // List of questions to be saved in local storage
        MessageService.subscribe($rootScope, "question-list", function (type, msg) {
            _(msg.questions).each(addOrUpdate);
        });

        // TODO: Add subscribe method to subscribe to changes (on arrival of new question(s))
        return {
            populate: populate,
            load: function () { return _(questions).values(); },
            count: function () { return Object.keys(questions).length; }
        }
    }])
    .config(["localStorageServiceProvider", function (localStorageServiceProvider) {
        localStorageServiceProvider
            .setPrefix("quizDb")
            .setStorageCookie(0, "/")
            .setNotify(false, false)
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
    }])
    .run(["QuestionService", function (QuestionService) { /* Make sure Question Service is loaded at run-time */ }]);