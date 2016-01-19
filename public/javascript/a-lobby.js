
console.log("Loading a-lobby.js");

var myApp = angular.module('lobbyModule',[]);

myApp.controller('LobbyController', function ($scope, $http) {
	console.log("Started loading controller");

    $scope.getLobbies = function () {
		$http.get('/lobbys').success(function (data) {
			$scope.lobbies = data;
			$scope.hasLobbies = data.length != 0
		});
    };
	$scope.getLobbies();
	setInterval($scope.getLobbies, 5000);
	console.log("Loaded controller");
});