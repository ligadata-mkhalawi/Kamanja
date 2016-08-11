'use strict';

angular.module('networkApp')
  .directive('header', ['$rootScope', 'serviceData', 'serviceConfig', function ($rootScope, serviceData, serviceConfig) {
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

        header.filterNodes = function($event){
          var chk = $event.target;
          $rootScope.$broadcast('filterNodesChanged', {
            type: chk.id.toLowerCase(),
            visible: chk.checked
          });
        };

        header.generateFilterList = function(viewObj){
          header.filterList = [];
          _.forEach(viewObj.SymbolClasses, function (item) {
            var type = serviceConfig.classImageColorMap[item.toLowerCase()];
            var filterObj = {
              displayName: item === 'Input' || item === 'Output' || item === 'Storage' ? item + " " + "Adapter" : item,
              imageName: type.image + '.inactive.' + type.extension,
              imageWidth: type.width,
              imageHeight: type.height,
              name: item
            };
            header.filterList.push(filterObj);
          });
        };

        header.setView = function (viewObj) {
          serviceData.setSelectedView(viewObj);
          header.generateFilterList(viewObj);
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
