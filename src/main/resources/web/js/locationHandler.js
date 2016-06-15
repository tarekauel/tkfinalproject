var locationHandler =(function(){

    var getPosition = function(callback) {
        navigator.geolocation.getCurrentPosition(function(position) {
            callback(position.coords.latitude, position.coords.longitude);
        });
    };


    return {
        isAvailable: "geolocation" in navigator,
        getPosition: getPosition
    }
})();