var scoreHandler = (function() {

    var refreshScores = function(message) {
        var scores = [];
        Object.keys(message.scores).forEach(function(key) {
            scores.push([message.scores[key], '<tr><td>' + key + '</td><td>' + message.scores[key] + '</td></tr>']);
        });
        scores = scores.sort(function(a, b) {return b[0] - a[0]});
        var table = ''
        scores.forEach(function(element) {
            table += element[1];
        })
        document.getElementById('scores').innerHTML = table;
    };

    messageHandler.setReceiver("score", refreshScores);
})();