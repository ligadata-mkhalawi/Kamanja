angular.module('flareApp')
  .controller('mainCtrl', ['$scope','$rootScope', function ($scope,$rootScope) {
    var main = this;
    main.showSideBar = true;
    main.showList = {
      'firstView': {
        'Coloring by Cluster': true,
        'Relationships': true,
        'Labels': true,
        'Applications': true,
        'Logs': true,
        'People': true,
        'Countries': true,
        'Organizations': true
      }
    };
    $rootScope.views = ['firstView'];
    $rootScope.currentView = 'firstView';
    $rootScope.showOptions = main.showList;
    main.getShowOption = function (option) {
      return main.showList[$rootScope.currentView][option];
    };
    main.toggleShowList = function(list) {
      main.showList[$rootScope.currentView][list] = !main.showList[$rootScope.currentView][list];
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
