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
      return "http://{serverIP}:{portNo}/kamanjauirest2/kamanjarest/GetViews".format({serverIP: serverIP, portNo: portNo});
    };

    service.getView = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest2/kamanjarest/GetView".format({serverIP: serverIP, portNo: portNo});
    };
    service.depthTraverse = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest2/kamanjarest/DepthTraverse".format({serverIP: serverIP, portNo: portNo});
    };
    service.getProperties = function(){
      return "http://{serverIP}:{portNo}/kamanjauirest2/kamanjarest/Properties".format({serverIP: serverIP, portNo: portNo});
    };

    return service;

  });
