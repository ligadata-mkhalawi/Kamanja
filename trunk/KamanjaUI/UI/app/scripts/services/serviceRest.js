'use strict';

angular.module('networkApp')
  .factory('serviceRest', function(){
    var service = {};
    var serverIP;
    var portNo;

    service.setConfig = function(configObj){
      serverIP = configObj.serverIP;
      portNo = configObj.portNo;
    };

    service.getViews = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest/kamanjarest/GetViews".format({serverIP: serverIP, portNo: portNo});
    };

    service.getView = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest/kamanjarest/GetView".format({serverIP: serverIP, portNo: portNo});
    };
    service.depthTraverse = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest/kamanjarest/DepthTraverse".format({serverIP: serverIP, portNo: portNo});
    };
    service.getProperties = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest/kamanjarest/Properties".format({serverIP: serverIP, portNo: portNo});
    };

    return service;

  });
