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
      controller: function(){

      }
    };
  });
