/**
 * Created by muhammad on 8/31/16.
 */

'use strict';

angular.module('flareApp')
  .factory('serviceData', function($rootScope, serviceUtil){
    return {
      getDummyData: function(id, callback){
        serviceUtil.httpRequest.query({
          url: './data/dummy.json',
          data: {}
        }, function(response){
          var obj = _.where(response, {id: id});
          callback(obj);
        });
      },
      getGraphData: function(callback){
        serviceUtil.httpRequest.query({
          url: './data/graph.json',
          data: {}
        }, function(response){
          callback(response[0]);
        });
      }
    }
  });
