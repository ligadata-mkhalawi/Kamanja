'use strict';

angular.module('networkApp')
  .factory('serviceRest', function(){
    var service = {};
    var serverIP;
    var portNo;
    var servicesServerIP;

    service.setConfig = function(configObj){
      serverIP = configObj.serverIP;
      portNo = configObj.portNo;
      servicesServerIP = servicesServerIP;
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

    service.uploadModel = function (fileType) {
      return "http://{serverIP}:{portNo}/api/model/{type}".format({
        serverIP: servicesServerIP, portNo: portNo, type: fileType
      });
    };

    service.getModelDetailedInfo = function (modelName) {
      return "http://{serverIP}:{portNo}/api/Model/{modelName}".format({
        serverIP: serverIP, portNo: portNo, modelName: modelName
      });
    };

    service.getModelsUrl = function () {
      return "http://{serverIP}:{portNo}/api/keys/Model".format({serverIP: servicesServerIP, portNo: portNo});
    };

    service.deleteModelUrl = function (modelName) {
      return "http://{serverIP}:{portNo}/api/model/{modelName}".format({
        serverIP: servicesServerIP,
        portNo: portNo,
        modelName: modelName
      });
    };

    return service;

  });
