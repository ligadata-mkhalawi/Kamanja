'use strict';

angular.module('networkApp')
  .directive('header', ['$rootScope', 'serviceData', function ($rootScope, serviceData) {
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/header.html',

      controllerAs: 'header',
      controller: function ($interval) {
        var header = this;
        header.listViews = [];

        $interval(function () {
          header.currentDate = Date.now();
        }, 1000);


        header.hideSideMenu = function () {
          $rootScope.$broadcast('closeSideMenu');
        };

        header.toggleShowStatus = function () {
          $rootScope.showStatus = !$rootScope.showStatus;
        };
        header.getShowStatus = function () {
          return $rootScope.showStatus;
        };

        header.getViews = function () {
          serviceData.getViews(function (response) {

            header.listViews = response.result;
            if (response.result)
              header.setView(_.find(response.result, {isDefault: true}));
          });
        };

        header.setView = function (viewObj) {
          serviceData.setSelectedView(viewObj);
          header.selectedView = serviceData.getSelectedViewName();
        };

        header.init = function(){
          serviceData.getConfig(function(){
            header.getViews();
          });
        };

        header.init();
      }
    };
  }]);
