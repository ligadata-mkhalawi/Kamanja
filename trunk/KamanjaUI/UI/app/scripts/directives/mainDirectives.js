'use strict';

angular.module('networkApp')
  .directive('footer', function ($rootScope) {
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/footer.html',
      controllerAs: 'footer',
      controller: function ($rootScope, $uibModal, serviceData, serviceConfig) {
        var footer = this;
        footer.footerList = [];
        var selectedModelIds = [];

        footer.hideSideMenu = function () {
          $rootScope.$broadcast('closeSideMenu');
        };

        footer.filterNode = function (item) {
          item.visible = !item.visible;
          if (item.visible) {
            item.style = {};
          } else {
            item.style = {'opacity': 0.5};
          }

          $rootScope.$broadcast('filterNodesChanged',
            {id: item.id, visible: item.visible, isSearchText: false});
        };

        $rootScope.$on('viewChanged', function (event, data) {
          footer.footerList = [];
          _.forEach(data.SymbolClasses, function (item) {
            var type = serviceConfig.classImageColorMap[item.toLowerCase()];
            var footerObj = {
              headerColor : type.headerColor,
              displayName: item === 'Input' || item === 'Output' || item === 'Storage' ? item + " " + "Adapter" : item,
              imageName: type.image + '.inactive.' + type.extension,
              imageWidth: type.width,
              imageHeight: type.height,
              id: item,
              visible: true,
              style: {},
              unselectUnselected: unselectUnselected
            };
            footer.footerList.push(footerObj);
          });
        });
        function unselectUnselected() {
          _.each(this.selectedModels,function(selectedModel){
            if (selectedModel.unSelect){
              $rootScope.$broadcast('nodeClicked', selectedModel.id);
              $rootScope.$broadcast('nodeUnSelected', selectedModel.id);
              selectedModel.unSelect = false;
            }
          });
        }
        $rootScope.$on('nodeClicked', function (event, data) {
          footer.selectedModels = [];
          if (selectedModelIds.indexOf(data) === -1) {
            selectedModelIds.push(data);
          } else {
            selectedModelIds.splice(selectedModelIds.indexOf(data), 1);
          }
          _.each(selectedModelIds, function (d) {
            footer.selectedModels.push(_.find(serviceData.getSelectedViewData().result, {id: d}));
          });
          _.each(footer.footerList, function (l) {
            l.selectedModels = _.filter(footer.selectedModels, function (m) {
              return m.class.toLowerCase() === l.id.toLowerCase();
            });
          });
        });

        // add new model
        footer.addNewModelModal = function(size){
          var modalInstance = $uibModal.open({
            animation: true,
            backdrop: true,
            templateUrl: 'views/tpl/addModelModal.tpl.html',
            controller: 'addNewModelModalCtrl',
            windowClass: 'addNewModelModal',
            size: size,
            resolve: {
              addModel: function () {
                return function (newModel) {

                }
              }
            }
          });
        };
      }
    };
  })
  .directive('modelDetails', function () {
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/modelDetails.html',
      controllerAs: 'modelDetails',
      controller: function ($rootScope, $filter, serviceData, serviceConfig) {
        var modelDetails = this;
        modelDetails.modelInfo = [];
        var selectedModelIds = [];
        modelDetails.currentNodeId = '';

        modelDetails.closeSideMenu = function () {
          $rootScope.$broadcast('closeSideMenu');
        }

        $rootScope.$on('closeSideMenu', function (event, data) {
          modelDetails.currentNodeId = '';
          toggleModelDetails(false);
        });

        $rootScope.$on('serviceError', function (event, data) {
          modelDetails.isError = true;
        });

        $rootScope.$on('nodeClicked', function (event, data) {
          modelDetails.selectedModels = [];
          if (selectedModelIds.indexOf(data) === -1) {
            selectedModelIds.push(data);
          } else {
            selectedModelIds.splice(selectedModelIds.indexOf(data), 1);
          }
          if (selectedModelIds.length === 0) {
            toggleModelDetails(false);
            modelDetails.currentNodeId = '';
          }
          else if (selectedModelIds.length >= 1) {
            _.each(selectedModelIds, function (d) {
              modelDetails.selectedModels.push(_.find(serviceData.getSelectedViewData().result, {id: d}));
            });
            data = selectedModelIds[selectedModelIds.length - 1];
            modelDetails.currentNodeId = data;
            toggleModelDetails(true);
            modelDetails.modelInfo = [];
            var nodeInfo = _.find(serviceData.getSelectedViewData().result, {id: data});
            if (nodeInfo.Type === 'V') {
              var type = serviceConfig.classImageColorMap[nodeInfo.class];
              modelDetails.headerColor = type.headerColor;
              modelDetails.imageName = type.image + '.' + type.extension;
              modelDetails.headerName = nodeInfo.Name;
              modelDetails.imageWidth = type.widthProperties;
              modelDetails.imageHeight = type.heightProperties;
            }
            serviceData.getProperties({
              "ViewName": serviceData.getSelectedViewName(),
              "RID": data
            }, function (response) {
              //modelDetails.modelInfo = response;
              addProperties(response);

            });
          }
          else {
          }
        });

        $rootScope.$on('edgeSelected', function (event, data) {
          if (modelDetails.currentNodeId === data) {
            toggleModelDetails(false);
            modelDetails.currentNodeId = '';
          }
          else {
            modelDetails.currentNodeId = data;
            toggleModelDetails(true);
          }
          modelDetails.modelInfo = [];
          var nodeInfo = _.find(serviceData.getSelectedViewData().result, {id: data});
          modelDetails.headerName = nodeInfo.Name;

          nodeInfo = _.find(serviceData.getSelectedViewData().result, {id: nodeInfo.out});
          var type;
          if (serviceData.getSelectedViewName().toLowerCase() === "dag") {
            type = serviceConfig.classImageColorMap['dag'];
          } else {
            type = serviceConfig.classImageColorMap[nodeInfo.class];
          }


          modelDetails.headerColor = type.headerColor;
          modelDetails.imageName = type.image + '.edge.' + type.extension;

          modelDetails.imageWidth = type.widthProperties;
          modelDetails.imageHeight = type.heightProperties;

          serviceData.getProperties({"ViewName": serviceData.getSelectedViewName(), "RID": data}, function (response) {
            //modelDetails.modelInfo = response;
            addProperties(response);

          });
        });

        function addProperties(result) {
          _.forOwn(result, function (value, key) {
            if (!key.startsWith('@')) {
              if (key.toLowerCase() === 'CreatedTime'.toLocaleLowerCase()) {
                modelDetails.modelInfo.push({key: key, value: $filter('date')(value, 'medium')});
              } else {
                modelDetails.modelInfo.push({key: key, value: value});
              }
            }
          });
        }
      }
    };
  });
