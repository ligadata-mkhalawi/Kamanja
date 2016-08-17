'use strict';

/**
 * @ngdoc overview
 * @name networkApp
 * @description
 * # networkApp
 *
 * Main module of the application.
 */
angular
  .module('networkApp', [
    'ngAnimate',
    'ui.router',
    'ngResource',
    'ui.bootstrap'
  ])
  .config(function ($stateProvider, $urlRouterProvider) {

    $urlRouterProvider.otherwise(function ($injector, $location) {
      var $state = $injector.get('$state');
      if ($location.$$path === '' || $location.$$path === '/') {
        $state.go('/');
      } else {
        $state.go('404');
      }
    });

    $stateProvider
      .state('/', {
        templateUrl: 'views/main.html',
        controller: 'MainCtrl as main'
      })
      .state('about', {
        templateUrl: 'views/about.html',
        controller: 'AboutCtrl as about'
      })
      .state('404', {
        templateUrl: '404.html'
      });
  });
