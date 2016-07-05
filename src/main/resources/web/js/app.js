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
            // Add or update a question inside the local db
            addOrUpdateDb = function (question) {
                var q = _(question).pick("questionId", "question", "answerA", "answerB", "answerC", "answerD", "correctAnswer", "pos");

                if (_(q).has('pos') && _(q.pos).has('value')) {
                    q.pos = q.pos.value;
                }

                var json = JSON.stringify(q);
                if (localStorageService.get(q.questionId) !== json) {
                    localStorageService.set(q.questionId, json);
                    questions[q.questionId] = q;

                    return true;
                }

                return false;
            }
            // Create/update a question on server side.
            // On creation: question will not be added to the local db immediatley because it requires the UUID from the server.
            addOrUpdate = function (question) {
                if (_(question).has('pos') && !_.isEmpty(question.pos) && !_(question.pos).has('value')) {
                    question.pos = { value: question.pos };
                }

                if (_(question).has('questionId') && question.questionId.length === 36) {
                    addOrUpdateDb(question);
                }

                MessageService.sendMessage({
                    type: "question-update",
                    question: question
                });
            },
            // Get a question from the local db by its UUID
            getById = function (questionId) {
                return JSON.parse(localStorageService.get(questionId));
            },
            // Ask server for known questions. Optionally, known UUIDs can be send to the server to leave them out in the reply.
            retrieve = function (excludeKnownIds) {
                excludeKnownIds = excludeKnownIds !== false;
                message = { type: "question-db", exclude: [] };
                if (excludeKnownIds) message.exclude = localStorageService.keys();
                MessageService.sendMessage(message);
            },
            // Reset the local database
            /*reset = function () {
                localStorageService.clearAll();
                questions = {};
            },
            sync = function () { },*/
            subscribe = function (scope, callback) {
                var handler = $rootScope.$on("question-service-update", function () {
                    callback.apply(scope);
                    scope.$apply();
                });
                scope.$on("$destroy", handler);
            };

        // Load questions from local storage
        _(localStorageService.keys()).each(function (questionId) {
            questions[questionId] = JSON.parse(localStorageService.get(questionId));
        });

        // Subscribe to new questions and insert/update them into the local storage on demand
        MessageService.subscribe($rootScope, "question", function (type, msg) {
            addOrUpdateDb(msg);
            $rootScope.$emit("question-service-update");
        });

        // Save list of questions (reply from server) in the local storage
        MessageService.subscribe($rootScope, "question-list", function (type, msg) {
            _(msg.questions).each(addOrUpdateDb);
            $rootScope.$emit("question-service-update");
        });

        retrieve();

        // TODO: Add subscribe method to subscribe to changes (on arrival of new question(s))
        return {
            get: getById,
            getQuestions: function () { return _(questions).values(); },
            // count: function () { return Object.keys(questions).length; },
            onUpdate: subscribe,
            addOrUpdate: addOrUpdate,
        }
    }])
    .directive('convertToNumber', function() {
        return {
            require: 'ngModel',
            link: function(scope, element, attrs, ngModel) {
                ngModel.$parsers.push(function(val) {
                    return parseInt(val, 10);
                });
                ngModel.$formatters.push(function(val) {
                    return '' + val;
                });
            }
        }
    })
    .filter("coordinates", function () {
        return function (input) {
            input = input || {};

            if (!_(input).has("latitude") || !_(input).has("longitude")) {
                return '[None]';
            }

            return "Lat: " + input.latitude + ", Long: " + input.longitude;
        }
    })
    .filter("numToChar", function () {
        return function (input) {
            if (input >= 0 && input < 26) {
                return String.fromCharCode(input + 65);
            }

            return "?";
        }
    })
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
            .when("/questions/create", {
                controller: "QuestionCreateCtrl",
                templateUrl: "partials/question.edit.html"
            })
            .when("/questions/:questionId", {
                controller: "QuestionEditCtrl",
                templateUrl: "partials/question.edit.html"
            })
            .otherwise("/login");
    }])
    .run(["QuestionService", function (QuestionService) { /* Make sure Question Service is loaded at run-time */ }]);