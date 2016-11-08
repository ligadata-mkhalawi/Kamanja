/**
 * @ngdoc function
 * @name networkApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the networkApp
 */

angular.module('networkApp')
  .controller('MainCtrl', ['$rootScope', 'serviceData', 'servicePrepare',
    function ($rootScope, serviceData, servicePrepare) {
      'use strict';
      var main = this;
      main.networkData = null;
      main.getShowStatus = function () {
        return $rootScope.showStatus;
      };

      function updateNetworkData(data) {
        main.selectedViewName = serviceData.getSelectedViewName();
        main.networkData = servicePrepare.viewToVis(serviceData.getSelectedViewData());
        if (data) {
          main.symbolClasses = data.SymbolClasses;
        }
      }

      $rootScope.$on('viewChanged', function (event, data) {
        updateNetworkData(data);
      });
      main.nodeDoubleClick = function (id) {
        serviceData.depthTraverse(id, function (response) {
          updateNetworkData();
        });
      };
      main.nodeClick = function (id) {
        // console.log("node", id);
        $rootScope.$broadcast('nodeClicked', id);
      };
      main.edgeClick = function (id) {
        // console.log("edge", id);
        $rootScope.$broadcast('edgeSelected', id);
      };
      main.groundClick = function () {
        // console.log('ground');
        $("#txtFilter").blur();
        $rootScope.$broadcast('closeSideMenu');
        $rootScope.$broadcast('groundSelected');
      };
    }]);
