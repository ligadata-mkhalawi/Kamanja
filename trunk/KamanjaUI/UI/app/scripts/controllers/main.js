/**
 * @ngdoc function
 * @name networkApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the networkApp
 */

angular.module('networkApp')
  .controller('MainCtrl', ['$rootScope', '$scope', 'serviceData', 'servicePrepare', 'serviceSocket',
    function ($rootScope, $scope, serviceData, servicePrepare, serviceSocket) {
      'use strict';
      var main = this;
      main.networkData = null;
      main.showFirst = true;
      main.tabs = [{title: 'tab 1',show:false},{title: 'tab 2',show:true}];
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
        updateNetworkData();
      });
      main.nodeDoubleClick = function (id) {
        serviceData.depthTraverse(id, function (response) {
          updateNetworkData();
        });
      };
      main.nodeClick = function (id) {
        console.log("node", id);
        $rootScope.$broadcast('nodeSelected', id);
      };
      main.edgeClick = function (id) {
        console.log("edge", id);
        $rootScope.$broadcast('edgeSelected', id);
      };
      main.groundClick = function () {
        console.log('ground');
        $rootScope.$broadcast('closeSideMenu');
        $rootScope.$broadcast('groundSelected');
      };
    }]);
