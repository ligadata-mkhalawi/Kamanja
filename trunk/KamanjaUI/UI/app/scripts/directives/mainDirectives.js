'use strict';

angular.module('networkApp')
  .directive('footer', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/footer.html',
      controllerAs: 'footer',
      controller: function ($rootScope, serviceData, serviceConfig) {
        var footer = this;
        footer.footerList = [];

        footer.hideSideMenu = function(){
          $rootScope.$broadcast('closeSideMenu');
        };

        $rootScope.$on('viewChanged', function (event, data) {
          footer.footerList = [];
          _.forEach(data.SymbolClasses, function (item) {
            var type = serviceConfig.classImageColorMap[item.toLowerCase()];
            var footerObj = {
              displayName: item === 'Input' || item === 'Output' || item === 'Storage' ? item + " " + "Adapters" : item,
              imageName: type.image + '.inactive.' + type.extension,
              imageWidth: type.width,
              imageHeight: type.height
            };
            footer.footerList.push(footerObj);
          });
        });


      }
    };
  })
  .directive('modelDetails', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/modelDetails.html',
      controllerAs: 'modelDetails',
      controller: function ($rootScope,$filter, serviceData, serviceConfig) {
        var modelDetails = this;
        modelDetails.modelInfo = [];

        modelDetails.currentNodeId = '';

        modelDetails.closeSideMenu = function(){
          $rootScope.$broadcast('closeSideMenu');
        }

        $rootScope.$on('closeSideMenu', function(event, data){
          modelDetails.currentNodeId = '';
          toggleModelDetails(false);
        });

        $rootScope.$on('serviceError', function(event, data){
          modelDetails.isError = true;
        });

        $rootScope.$on('nodeSelected', function (event, data) {
          if(modelDetails.currentNodeId === data) {
            toggleModelDetails(false);
            modelDetails.currentNodeId = '';
          }
          else{
            modelDetails.currentNodeId = data;
            toggleModelDetails(true);
          }
          modelDetails.modelInfo = [];
          var nodeInfo = _.find(serviceData.getSelectedViewData().result, {id: data});
          if (nodeInfo.Type === 'V') {
            var type = serviceConfig.classImageColorMap[nodeInfo.class];
            modelDetails.headerColor = type.headerColor;
            modelDetails.imageName = type.image + '.' + type.extension;
            modelDetails.headerName = nodeInfo.Name;
            modelDetails.imageWidth = type.width;
            modelDetails.imageHeight = type.height;
          }
          serviceData.getProperties({"ViewName": serviceData.getSelectedViewName(), "RID": data}, function (response) {
            //modelDetails.modelInfo = response;
            addProperties(response);

          });
        });

        $rootScope.$on('edgeSelected', function (event, data) {
          if(modelDetails.currentNodeId === data) {
            toggleModelDetails(false);
            modelDetails.currentNodeId = '';
          }
          else{
            modelDetails.currentNodeId = data;
            toggleModelDetails(true);
          }
          modelDetails.modelInfo = [];
          var nodeInfo = _.find(serviceData.getSelectedViewData().result, {id: data});
          modelDetails.headerName = nodeInfo.Name;

          nodeInfo = _.find(serviceData.getSelectedViewData().result, {id: nodeInfo.out});
          var type;
          if(serviceData.getSelectedViewName().toLowerCase() === "dag"){
            type = serviceConfig.classImageColorMap['dag'];
          }else{
            type = serviceConfig.classImageColorMap[nodeInfo.class ];
          }


          modelDetails.headerColor = type.headerColor;
          modelDetails.imageName = type.image + '.edge.' + type.extension;

          modelDetails.imageWidth = type.width;
          modelDetails.imageHeight = type.height;

          serviceData.getProperties({"ViewName": serviceData.getSelectedViewName(), "RID": data}, function (response) {
            //modelDetails.modelInfo = response;
            addProperties(response);

          });
        });

        function addProperties(result){
          _.forOwn(result, function (value, key) {
            if (!key.startsWith('@')) {
              if(key.toLowerCase() === 'CreatedTime'.toLocaleLowerCase())
              {
                modelDetails.modelInfo.push({key: key, value: $filter('date')(value, 'medium')});
              }else {
                modelDetails.modelInfo.push({key: key, value: value});
              }
            }
          });
        }
      }
    };
  });
