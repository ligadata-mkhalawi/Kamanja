angular.module('flareApp')
  .controller('mainCtrl', ['$scope','$rootScope', function ($scope,$rootScope) {
    var main = this;
    main.showSideBar = true;
    main.showList = {
      'Coloring by Cluster':true,
      'Relationships': true,
      'Labels':true,
      'Applications':true
    };
    $rootScope.showOptions = main.showList;

    main.nodeClicked = function(id){
      $rootScope.$broadcast('nodeClicked', id);
    };

    main.toggleShowList = function(list) {
      switch (list){
        case 'Labels':
          $rootScope.$broadcast('labelsToggled');
          break;
        case 'Applications':
          $rootScope.$broadcast('applicationsToggled');
          break;
      }
      main.showList[list] = !main.showList[list];
    };

    $scope.$on('sideBarToggled', function() {
      main.showSideBar = !main.showSideBar;
    });
  }]);
