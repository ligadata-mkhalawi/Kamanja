/**
 * Created by muhammad on 8/29/16.
 */

'use strict';
angular.module('flareApp')
  .directive('nodeDetails', ['$rootScope', 'serviceData', function($rootScope, serviceData){
    return {
      restrict: 'E',
      templateUrl: 'views/tpl/nodeDetails.html',
      controllerAs: 'nodeDetails',
      controller: function($rootScope){
        var nodeDetails = this;

        $rootScope.$on('nodeClicked', function(event, data){
          toggleNodeDetails(true);
          nodeDetails.applicationName = data;
          serviceData.getDummyData(data, function(result){
            nodeDetails.result = result[0];
          });
        });

      }
    }
  }]);
