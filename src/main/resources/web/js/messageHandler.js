var messageHandler = (function() {
    var receivers = {

    };

    var sendMessage = function(message) {
        // forward message to backend and append location information
        locationHandler.getPosition(function(long, lat) {
            message._pos = {
                long: long,
                lat: lat
            };
            connection.send(JSON.stringify(message));
        });
    };

    var receiveMessage = function (message) {
        var json = JSON.parse(message.data);
        console.log('Message: ', json);
        if (!json.type) {
            console.error("Received invalid message, unknown type " + json.type)
        } else {
            var type = json.type;
            if (receivers[type] instanceof Function) {
                receivers[type](json)
            } else {
                console.error("Received message of type " + json.type + " but no receiver is known");
            }
        }
    };

    var setReceiver = function(type, callback) {
        receivers[type] = callback;
    };


    var connection = new WebSocket('ws://' + location.hostname + ':' + (parseInt(location.port) + 1));
    connection.onmessage = receiveMessage;


    return {
        sendMessage: sendMessage,
        setReceiver: setReceiver
    };
})();