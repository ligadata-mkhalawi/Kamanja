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

    return service;
  });
