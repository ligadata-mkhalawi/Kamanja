angular.module('flareApp')
  .controller('mainCtrl', ['$scope','$rootScope', function ($scope,$rootScope) {
    var main = this;
    main.showSideBar = true;
    main.showList = {
      'Coloring by Cluster':true,
      'Relationships': true,
      'Labels':true,
      'Applications':true,
      'Logs':true,
      'People':true,
      'Countries':true,
      'Organizations':true
    };
    $rootScope.showOptions = main.showList;

    main.nodeClicked = function(id){
      $rootScope.$broadcast('nodeClicked', id);
    };

    main.toggleShowList = function(list) {
      main.showList[list] = !main.showList[list];
      switch (list){
        case 'Labels':
          $rootScope.$broadcast('labelsToggled');
          break;
        case 'Applications':
        case 'Logs':
        case 'People':
        case 'Countries':
        case 'Organizations':
          $rootScope.$broadcast('showOptionsToggled');
          break;
      }
    };

    $scope.$on('sideBarToggled', function() {
      main.showSideBar = !main.showSideBar;
    });
  }]);
