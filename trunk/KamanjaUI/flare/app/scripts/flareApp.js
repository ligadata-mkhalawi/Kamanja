/**
 * Created by muhammad on 8/18/16.
 */
'use strict'
angular.module('flareApp', ['ngAnimate', 'ui.router','ui.bootstrap'])
  .config(function ($stateProvider, $urlRouterProvider) {
    $urlRouterProvider.otherwise(function ($injector, $location) {
      var $state = $injector.get('$state');
      if ($location.$$path === '' || $location.$$path === '/') {
        $state.go('/');
      } else
        $state.go('404');
    });

    $stateProvider
      .state('/', {
        templateUrl: 'views/main.html',
        controller: 'mainCtrl as main'
      })
      .state('404', {
        templateUrl: 'views/404.html'
      });
  });
