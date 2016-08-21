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
        header.isSearchBoxShown = false;
        header.isFilterShown = false;

        $interval(function () {
          header.currentDate = Date.now();
        }, 1000);

        $rootScope.$on('configData', function(event, data){
          header.showFilter = data.showFilter;
          header.showStatus = data.socketOn;
        });

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

        header.filterChecked = function($event){
          header.filterNodes();
        };

        header.filterNodes = function(){
          $rootScope.$broadcast('filterNodesChanged',
            {filterList: this.filterList, searchText: header.selectedSearch});
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
              name: item,
              checked: true
            };
            header.filterList.push(filterObj);
          });
        };

        header.setView = function (viewObj) {
          serviceData.setSelectedView(viewObj);
          header.generateFilterList(viewObj);
          header.selectedView = serviceData.getSelectedViewName();
          $rootScope.$on('viewChanged', function(event, data){
            header.selectedViewData = serviceData.getSelectedViewData().result;
          });
        };

        header.onSearchSelectChange = function($item, $model, $label, $event){
          console.log($item);
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
