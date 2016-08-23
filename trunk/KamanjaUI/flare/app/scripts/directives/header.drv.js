/**
 * Created by muhammad on 8/18/16.
 */
'use strict'

angular.module('flareApp')
  .directive('header', function ($rootScope) {
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/header.html',
      controllerAs: 'header',
      controller: ['$scope','$rootScope',function($scope, $rootScope){
        $scope.toggleSideBar = function(){
          $rootScope.$broadcast('sideBarToggled');
        };
      }]
    };
  });
