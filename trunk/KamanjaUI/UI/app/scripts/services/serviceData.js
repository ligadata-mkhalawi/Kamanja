'use strict';

angular.module('networkApp')
  .factory('serviceData', function ($rootScope, serviceUtil, serviceRest) {
    var service = {};

    var selectedViewMetaData = {};
    var selectedViewData;

    service.getConfig = function(callback){
      serviceUtil.httpRequest.get({
        url: './config.json',
        data: {}
      }, function(response){
        serviceRest.setConfig(response);
        $rootScope.$broadcast('configData', response);
        callback();
      });
    };



    service.getViews = function (callback) {
      serviceUtil.httpRequest.get({
        url: serviceRest.getViews()
      }, function (response) {
        if (response.errors) {
          $rootScope.$broadcast('serviceError');
          return;
        }
        callback(response);
      });
    };
    service.setSelectedView = function (view) {
      selectedViewMetaData = view;
      service.getView(view.ViewName, function (response) {
        if (response.errors) {
          $rootScope.$broadcast('serviceError');
          return;
        }
        selectedViewData = response;
        $rootScope.$broadcast('viewChanged', service.getSelectedViewMetaData());
      });
    };
    service.getSelectedViewData = function () {
      return selectedViewData;
    }
    service.getSelectedViewMetaData = function () {
      return selectedViewMetaData;
    };

    service.getSelectedViewName = function () {
      return selectedViewMetaData[selectedViewMetaData.MainQuery_parameters.ViewName];
    };

    service.getView = function (viewName, callback) {
      serviceUtil.httpRequest.save({
        data: {parameters: {ViewName: viewName}},
        url: serviceRest.getView()
      }, function (response) {
        if (response.errors) {
          $rootScope.$broadcast('serviceError');
          return;
        }
        callback(response);
      });
    };
    service.depthTraverse = function (rid, callback) {
      var viewName = service.getSelectedViewName();
      serviceUtil.httpRequest.save({
        data: {parameters: {ViewName: viewName, RID: rid}},
        url: serviceRest.depthTraverse()
      }, function (response) {
        if (response.errors) {
          $rootScope.$broadcast('serviceError');
          return;
        }
        var change = false;
        _.each(response.result, function (r) {
          if (!_.find(selectedViewData.result, function (p) {
              return r['@rid'] === p['@rid'] && r.Type === p.Type;
            })) {
            selectedViewData.result.push(r);
            change = true;
          }
        });
        if (change) {
          callback();
        }
      });
    };

    service.getProperties = function (dataObj, callback) {
      serviceUtil.httpRequest.save({
        url: serviceRest.getProperties(),
        data: {"parameters": dataObj}
      }, function (response) {
        if (response.errors) {
          $rootScope.$broadcast('serviceError');
          return;
        }
        callback(response.result[0]);
      });
    };

    /* ----- Kamanja APIs -------- */

    service.addModel = function (data, fileType, headerObj, callback) {
      //            https://github.com/caolan/async
      async.waterfall([
        function (callback) {
          serviceUtil.httpRequest.Save({
              url: serviceRest.uploadModel(fileType),
              headers: headerObj,
              data: data
            },
            function (response) {
              if (response.APIResults["Status Code"] === -1) {
                serviceUtil.showErrorNotification('Add {modelName} Model'.format({modelName: fileType.toUpperCase()}), response.APIResults["Result Description"]);
                callback(response);
              } else {
                serviceUtil.showSuccessNotification('Add {modelName} Model'.format({modelName: fileType.toUpperCase()}),
                  response.APIResults["Result Description"]);
                callback(response);
              }
            });
        }
      ], function (result) {
        callback(result);
      });
    };

    service.deleteModel = function (model, callback) {
      serviceUtil.httpRequest.delete({
        url: serviceRest.deleteModelUrl(model.originalName),
        data: {}
      }, function (response) {
        if (response.APIResults["Status Code"] === 0) {
          //models = _.without(models, _.findWhere(models, {id: model.id}));
          serviceUtil.showSuccessNotification('Delete Model', response.APIResults["Result Description"]);
          serviceUtil.httpRequest.Save({
            url: '/deleteModel',
            data: {
              model: model,
              token: authenticationService.getToken()
            }
          }, function (response) {
            if (response.result.error) {
              serviceUtil.showErrorNotification('Deleting Model', response.result.error);
            } else {
              callback(response.result);
            }
          });
        } else {
          serviceUtil.showErrorNotification('Delete Model', response.APIResults["Result Description"]);
        }
      });
    };

    service.updateModel = function (fileInfo, modelInfo, headerObj, callback, errorCallback) {
      async.waterfall([
        function (callback) {
          if (fileInfo.fileType) {
            serviceUtil.httpRequest.put({
                url: serviceRest.uploadModel(fileInfo.fileType),
                headers: headerObj,
                data: fileInfo.fileData
              },
              function (response) {
                if (response.APIResults["Status Code"] === -1) {
                  serviceUtil.showErrorNotification('Update {modelName} Model'.format({modelName: fileInfo.fileType.toUpperCase()}), response.APIResults["Result Description"]);
                  errorCallback();
                }
                else {
                  serviceUtil.showSuccessNotification('{modelName} Model Updated'.format({modelName: fileInfo.fileType.toUpperCase()}),
                    response.APIResults["Result Description"]);
                  callback(response);
                }
              });
          } else {
            callback(null);
          }
        }
      ], function (result) {
        if (result) {
          modelInfo.updatedName = result.APIResults["Result Description"].split(':')[1];
        }
        serviceUtil.httpRequest.Save({
          url: '/updateModel',
          headers: {},
          data: {
            model: modelInfo,
            token: authenticationService.getToken()
          }
        }, function (response) {
          //modelInfo.name = modelInfo.name.split('.')[modelInfo.name.split('.').length - 2];
          callback(response.result);
        });
      });
    };

    service.updatePmml = function (fileInfo, modelInfo, headerObj, callback) {
      async.waterfall([
        function (callback) {
          serviceUtil.httpRequest.put({
              url: serviceRest.uploadModel('pmml'),
              headers: {modelconfig: 'system.DecisionTreeIris,0.2.0', tenantid: "tenant1"},
              data: fileInfo.fileData
            },
            function (response) {
              if (response.APIResults["Status Code"] === -1) {
                serviceUtil.showErrorNotification('Update PMML', response.APIResults["Result Description"]);
                callback(null);
              }
              else {
                serviceUtil.showSuccessNotification('{modelName} Model Updated'.format({modelName: fileInfo.fileType.toUpperCase()}),
                  response.APIResults["Result Description"]);
                callback(response);
              }
            });
        }
      ], function (result) {
        serviceUtil.httpRequest.Save({
          url: '/updateModel',
          headers: {},
          data: {
            model: modelInfo,
            token: authenticationService.getToken
          }
        }, function (response) {
          callback(response.result);
        });
      });
    };

    service.addPmml = function (data, fileType, headerObj, callback) {
      //            https://github.com/caolan/async
      async.waterfall([
        function (callback) {
          serviceUtil.httpRequest.Save({
              url: serviceRest.uploadModel('pmml'),
              headers: headerObj,
              data: data
            },
            function (response) {
              if (response.APIResults["Status Code"] === -1) {
                serviceUtil.showErrorNotification('Add PMML Model', response.APIResults["Result Description"]);
                callback(response);
              } else {
                serviceUtil.showSuccessNotification('Add PMML Model',
                  response.APIResults["Result Description"]);
                callback(response);
              }
            });
        }
      ], function (result) {
        callback(result);
      });
    };

    service.addDefinition = function (data, callback) {
      //            https://github.com/caolan/async
      async.waterfall([
        function (callback) {
          serviceUtil.httpRequest.put({
              url: serviceRest.uploadDefinition(),
              data: data
            },
            function (response) {
              if (response.APIResults["Status Code"] === -1) {
                serviceUtil.showErrorNotification('Upload Model Config', response.APIResults["Result Description"]);
                callback(response);
              } else {
                callback(response);
              }
            });
        }
      ], function (result) {
        callback(result);
      });
    };

    /*---------- End Kamanja API --------------------*/

    return service;
  });
