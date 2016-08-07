/**
 * Created by muhammad on 7/19/16.
 */
'use strict'
angular.module('networkApp')
  .factory('serviceSocket', function(){
    var service = {};

    var wsStatusUrl = 'ws://54.176.225.148:7080/v2/broker/?topics=testmessageevents_1';
    var wsStatus;

    service.connectStatus = function(callback){
      if ('WebSocket' in window) {

        wsStatus = new WebSocket(wsStatusUrl);
      } else if ('MozWebSocket' in window) {
        wsStatus = new MozWebSocket(wsStatusUrl);
      } else {
        console.log('websocke is not supported');
        return;
      }

      wsStatus.onopen = function () {
        console.log('Open Status!');
      };

      wsStatus.onmessage = function (event) {
        callback(event.data);
      };


      wsStatus.onclose = function () {

        service.disconnectStatus();
        console.log('Close Status!');
      };

      wsStatus.onerror = function (event) {
        console.log('Error Status!');
      };
    };

    service.disconnectStatus = function () {
      if (wsStatus != null) {
        wsStatus.close();
        wsStatus = null;
      }
      console.log('Disconnect Status!');
    };

    return service;
  });
