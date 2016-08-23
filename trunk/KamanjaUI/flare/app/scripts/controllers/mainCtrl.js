angular.module('flareApp')
  .controller('mainCtrl', ['$scope', function ($scope) {
    var main = this;
    main.showSideBar = true;
    main.showList = {
      'Coloring by Cluster':true,
      'Relationships': true
    };
    main.toggleShowList = function(list) {
      console.log(main.showList[list]);
      main.showList[list] = !main.showList[list];
    };
    $scope.$on('sideBarToggled', function() {
      main.showSideBar = !main.showSideBar;
    });
  }]);
