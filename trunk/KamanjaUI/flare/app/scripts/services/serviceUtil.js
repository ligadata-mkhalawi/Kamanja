/**
 * Created by muhammad on 8/31/16.
 */
'use strict';

angular.module('flareApp')
  .factory('serviceUtil', function ($rootScope, $resource, $http) {
    var service = {};
    service.httpRequest = {};

    service.httpRequest.get = function (Obj, callback) {
      _.each($http.defaults.headers.common, function (val, key, obj) {
        if (key !== "Accept") {
          delete $http.defaults.headers.common[key];
        }
      });
      if (Obj.headers) {
        _.each(_.keys(Obj.headers), function (key) {
          $http.defaults.headers.common[key] = Obj.headers[key];
        });
      }
      var resourceObj = ConstructResource(Obj.url, {method: 'GET'});
      var result = resourceObj.get(Obj.data, function () {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }

        callback(result);
      }, function (error) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        console.log('Http', error.message);
      });

    };

    service.httpRequest.query = function (Obj, callback) {
      _.each($http.defaults.headers.common, function (val, key, obj) {
        if (key !== "Accept") {
          delete $http.defaults.headers.common[key];
        }
      });
      if (Obj.headers) {
        _.each(_.keys(Obj.headers), function (key) {
          $http.defaults.headers.common[key] = Obj.headers[key];
        });
      }
      var resourceObj = ConstructResource(Obj.url);
      Obj.isArray = false;
      resourceObj.data = Obj.data;
      var result = resourceObj.query(Obj.data, function () {

        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }

        callback(result);
      }, function (error) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        console.log('Http', error.message);
      });
    };

    service.httpRequest.put = function (Obj, callback) {
      _.each($http.defaults.headers.common, function (val, key, obj) {
        if (key !== "Accept") {
          delete $http.defaults.headers.common[key];
        }
      });
      var resourceObj;
      if (Obj.headers) {
        _.each(_.keys(Obj.headers), function (key) {
          $http.defaults.headers.common[key] = Obj.headers[key];
        });

      }
      resourceObj = ConstructResource(Obj.url, {update: {method: 'PUT', cache: false}});
      //resourceObj.data = Obj.data;
      var result = resourceObj.save(Obj.data).$promise.then(function (res) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        callback(res);
      }, function (error) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        //alert('Http', error.data);
      });
    };

    service.httpRequest.save = function (Obj, callback) {
      _.each($http.defaults.headers.common, function (val, key, obj) {
        if (key !== "Accept") {
          delete $http.defaults.headers.common[key];
        }
      });
      var resourceObj;
      if (Obj.headers) {
        _.each(_.keys(Obj.headers), function (key) {
          $http.defaults.headers.common[key] = Obj.headers[key];
        });

      }
      resourceObj = ConstructResource(Obj.url, {method: 'POST', cache: false});

      var result = resourceObj.save(Obj.data).$promise.then(function (res) {
        if (res.error) {
          console.log('', 'Server error');
        }
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        callback(res);
      }, function (error) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        //alert('Http', error.data);
      });
    };

    service.httpRequest.delete = function (Obj, callback) {
      _.each($http.defaults.headers.common, function (val, key, obj) {
        if (key !== "Accept") {
          delete $http.defaults.headers.common[key];
        }
      });
      var resourceObj;
      if (Obj.headers) {
        _.each(_.keys(Obj.headers), function (key) {
          $http.defaults.headers.common[key] = Obj.headers[key];
        });
      }
      resourceObj = ConstructResource(Obj.url, {method: 'DELETE', cache: false});
      var result = resourceObj.delete(Obj.data).$promise.then(function (res) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
        callback(res);
      }, function (error) {
        if (Obj.headers) {
          _.each(_.keys(Obj.headers), function (key) {
            delete $http.defaults.headers.common[key];
          });
        }
      });
    };

    var ConstructResource = function (url, actionOptions) {
      return $resource(url, {}, actionOptions);
    };

    return service;
  });

